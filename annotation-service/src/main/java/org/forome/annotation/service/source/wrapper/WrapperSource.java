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

package org.forome.annotation.service.source.wrapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minidev.json.JSONArray;
import org.forome.annotation.annotator.AnnotationConsole;
import org.forome.annotation.service.source.struct.Record;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.utils.Statistics;
import org.forome.astorage.core.data.Conservation;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WrapperSource implements Source {

	private final static Logger log = LoggerFactory.getLogger(AnnotationConsole.class);

	private final Source source;

	private final Cache<String, Object> cache;

	private final Map<WrapperSourceType, Statistics> statistics;

	public WrapperSource(Source source) {
		this.source = source;
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.build();

		this.statistics = new HashMap<>();
		for (WrapperSourceType type : WrapperSourceType.values()) {
			statistics.put(type, new Statistics());
		}
	}

	@Override
	public Record getRecord(Position position) {
		try {
			Optional<Record> value = (Optional<Record>) cache.get(getCacheKey(WrapperSourceType.RECORD, position), () -> callable(WrapperSourceType.RECORD, () -> source.getRecord(position)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public Sequence getFastaSequence(Interval interval) {
		try {
			Optional<Sequence> value = (Optional<Sequence>) cache.get(getCacheKey(WrapperSourceType.FASTA, interval), () -> callable(WrapperSourceType.FASTA, () -> source.getFastaSequence(interval)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public Conservation getConservation(Position position) {
		try {
			Optional<Conservation> value = (Optional<Conservation>) cache.get(getCacheKey(WrapperSourceType.CONSERVATION, position), () -> callable(WrapperSourceType.CONSERVATION, () -> source.getConservation(position)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public JSONArray getGnomad(Position position) {
		try {
			Optional<JSONArray> value = (Optional<JSONArray>) cache.get(getCacheKey(WrapperSourceType.GNOMAD, position), () -> callable(WrapperSourceType.GNOMAD, () -> source.getGnomad(position)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public JSONArray getDbSNP(Interval interval) {
		try {
			Optional<JSONArray> value = (Optional<JSONArray>) cache.get(getCacheKey(WrapperSourceType.DBSNP, interval), () -> callable(WrapperSourceType.DBSNP, () -> source.getDbSNP(interval)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public JSONArray getDbNSFP(Interval interval) {
		try {
			Optional<JSONArray> value = (Optional<JSONArray>) cache.get(getCacheKey(WrapperSourceType.DBNSFP, interval), () -> callable(WrapperSourceType.DBNSFP, () -> source.getDbNSFP(interval)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public JSONArray getSpliceAI(Interval interval) {
		try {
			Optional<JSONArray> value = (Optional<JSONArray>) cache.get(getCacheKey(WrapperSourceType.SPLICEAI, interval), () -> callable(WrapperSourceType.SPLICEAI, () -> source.getSpliceAI(interval)).call());
			return value.orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static String getCacheKey(WrapperSourceType type, Position position){
		return type.name() + position.toString();
	}

	private static String getCacheKey(WrapperSourceType type, Interval interval){
		return type.name() + interval.toString();
	}

	private Callable callable(WrapperSourceType type, Callable callable) {
		return () -> {
			long t1 = System.currentTimeMillis();
			Optional result;
			try {
				result = Optional.ofNullable(callable.call());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			statistics.get(type).addTime(System.currentTimeMillis() - t1);
			return result;
		};
	}

	public boolean isEmptyStatistics() {
		return (statistics.values().stream().mapToInt(istatistics -> istatistics.count.get()).sum() == 0);
	}

	public void printStatistics() {
		for (Map.Entry<WrapperSourceType, Statistics> entry : statistics.entrySet()) {
			WrapperSourceType type = entry.getKey();
			Statistics iStatistics = entry.getValue();

			if (iStatistics.count.get() == 0) continue;
			log.debug("{}: {}", type, iStatistics.getStat());
		}
	}
}
