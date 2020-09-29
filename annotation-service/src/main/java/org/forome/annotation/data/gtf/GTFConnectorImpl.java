/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.data.gtf;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gtf.datasource.GTFDataSource;
import org.forome.annotation.data.gtf.datasource.mysql.GTFDataConnector;
import org.forome.annotation.data.gtf.mysql.struct.GTFRegion;
import org.forome.annotation.data.gtf.mysql.struct.GTFResult;
import org.forome.annotation.data.gtf.mysql.struct.GTFResultLookup;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;
import org.forome.annotation.utils.Statistics;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GTFConnectorImpl implements GTFConnector {

	private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

	private final GTFDataSource gtfDataSource;

	//	private final DatabaseConnector databaseConnector;
	private final GTFDataConnector gtfDataConnector;

//	private final LiftoverConnector liftoverConnector;

	private final ExecutorService threadPoolGTFExecutor;

	public final Statistics statisticCds = new Statistics();

	public GTFConnectorImpl(
			GTFDataSource gtfDataSource,
//            DatabaseConnectService databaseConnectService,
//            GTFConfigConnector gtfConfigConnector,
			LiftoverConnector liftoverConnector,
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler
	) throws Exception {
		this.gtfDataSource = gtfDataSource;

//        this.databaseConnector = new DatabaseConnector(databaseConnectService, gtfConfigConnector);
//		this.gtfDataConnector = new GTFDataConnector(databaseConnector);
		gtfDataConnector = (GTFDataConnector) gtfDataSource;

//		this.liftoverConnector = liftoverConnector;

		threadPoolGTFExecutor = new DefaultThreadPoolExecutor(
				MAX_THREAD_COUNT,
				MAX_THREAD_COUNT,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				"GnomadExecutorQueue",
				uncaughtExceptionHandler
		);
	}

	@Override
	public CompletableFuture<GTFResult> request(Assembly assembly, String chromosome, long position) {
		CompletableFuture<GTFResult> future = new CompletableFuture();
		threadPoolGTFExecutor.submit(() -> {
			try {
				GTFResult result = gtfDataConnector.getGene(assembly, chromosome, position);
				future.complete(result);
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	@Override
	public CompletableFuture<GTFRegion> getRegion(AnfisaExecuteContext context, Assembly assembly, Position position, String transcript) {
		CompletableFuture<GTFRegion> future = new CompletableFuture();
		threadPoolGTFExecutor.submit(() -> {
			try {
				Object[] result = lookup(context, assembly, position, transcript);
				future.complete((GTFRegion) result[1]);
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	@Override
	public CompletableFuture<List<GTFResultLookup>> getRegionByChromosomeAndPositions(AnfisaExecuteContext context, String chromosome, long[] positions) {
		CompletableFuture<List<GTFResultLookup>> future = new CompletableFuture();
		threadPoolGTFExecutor.submit(() -> {
			try {
				List<GTFResultLookup> result = lookupByChromosomeAndPositions(context, chromosome, positions);
				future.complete(result);
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	@Override
	public List<GTFTranscriptRow> getTranscriptRows(Assembly assembly, String transcript) {
		return gtfDataConnector.getTranscriptRows(assembly, transcript);
	}

	@Override
	public Object[] lookup(AnfisaExecuteContext context, Assembly assembly, Position position, String transcript) {
//		List<GTFTranscriptRow> rows = gtfDataSource.lookup(context, assembly, position, transcript);
//		if (rows == null) return null;
//		return lookup(position.value, rows);

		List<GTFTranscriptRow> rows = gtfDataConnector.getTranscriptRows(assembly, transcript);
		if (rows.isEmpty()) return null;

		return lookup(position, rows);
	}

	public List<GTFResultLookup> lookupByChromosomeAndPositions(AnfisaExecuteContext context, String chromosome, long[] positions) {
		Assembly assembly = context.anfisaInput.mCase.assembly;
		List<GTFResultLookup> result = new ArrayList<>();

		List<String> transcripts = gtfDataConnector.getTranscriptsByChromosomeAndPositions(assembly, chromosome, positions);
		for (String transcript : transcripts) {
			for (long position : positions) {
				List<GTFTranscriptRow> rows = gtfDataConnector.getTranscriptRows(assembly, transcript);
				if (rows.isEmpty()) continue;

				Object[] iResult = lookup(context, context.anfisaInput.mCase.assembly, new Position(Chromosome.of(chromosome), (int) position), transcript);
				GTFRegion region = (GTFRegion) iResult[1];
				result.add(new GTFResultLookup(transcript, rows.get(0).gene, position, region.region, region.indexRegion));
			}
		}

		return result;
	}

	public Object[] lookup(Position position, List<GTFTranscriptRow> rows) {
		int pos = position.value;

		long inf = rows.get(0).start;
		if (pos < inf) {
			return new Object[]{ (inf - pos), GTFRegion.UPSTREAM };
		}

		long sup = rows.get(rows.size() - 1).end;
		if (pos > sup) {
			return new Object[]{ (pos - sup), GTFRegion.DOWNSTREAM };
		}

		List<Integer> a = new ArrayList<>();
		for (GTFTranscriptRow row : rows) {
			a.add(row.start);
			a.add(row.end);
		}

		//Аналог: i = bisect.bisect(a, pos)
		int lo = 0;
		int hi = a.size();
		while (lo < hi) {
			int mid = (lo + hi) / 2;
			if (pos < a.get(mid)) {
				hi = mid;
			} else {
				lo = mid + 1;
			}
		}
		int i = lo;

		long d;
		if (pos == inf || pos == sup) {
			d = 0;
		} else {
			d = Math.min(pos - a.get(i - 1), a.get(i) - pos);
		}

		long index;
		String region;
		if ((i % 2) == 1) {
			index = (i + 1) / 2;
			region = "exon";
		} else {
			index = i / 2;
			region = "intron";
		}

		return new Object[]{ d, new GTFRegion(region, (int) index), rows.size() };
	}

	public Set<String> getCdsTranscript(Assembly assembly, Variant variant) {
		long t1 = System.currentTimeMillis();
		try {
			return gtfDataSource.getCdsTranscript(assembly, variant);
		} finally {
			statisticCds.addTime(System.currentTimeMillis() - t1);
		}
	}

	@Override
	public void close() {
		gtfDataSource.close();
	}
}
