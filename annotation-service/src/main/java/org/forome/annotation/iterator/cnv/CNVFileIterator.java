/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.iterator.cnv;

import com.google.common.collect.ImmutableList;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.mavariant.MAVariantCNV;
import org.forome.annotation.struct.variant.cnv.GenotypeCNV;
import org.forome.annotation.struct.variant.cnv.VariantCNV;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CNVFileIterator implements AutoCloseable {

	private static Pattern PATTERN_SAMPLES = Pattern.compile(
			"^(.*)#(.*)Samples(.*):(.*)$", Pattern.CASE_INSENSITIVE
	);

	private static Pattern PATTERN_HEAD_DATA = Pattern.compile(
			"^(.*)#(.*)CHROM(.*)$", Pattern.CASE_INSENSITIVE
	);

	private static final String COLUMN_CHROM = "CHROM";
	private static final String COLUMN_START = "START";
	private static final String COLUMN_END = "END";
	private static final String COLUMN_EXON_NUM = "EXON_NUM";
	private static final String COLUMN_TRANSCRIPT = "TRANSCRIPT";
	private static final String COLUMN_GT = "GT";
	private static final String COLUMN_LO = "LO";

	private final InputStream inputStream;
	private final BufferedReader bufferedReader;

	private List<String> samples;
	private Map<String, Integer> columns;

	private MAVariantCNV nextVariant;

	private CNVFileRecord nextProcessedRecord;

	public CNVFileIterator(Path pathCnv) {
		try {
			this.inputStream = Files.newInputStream(pathCnv);
			this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("#")) {
					Matcher matcherSamples = PATTERN_SAMPLES.matcher(line);
					if (matcherSamples.matches()) {
						samples = Arrays.stream(
								matcherSamples.group(4).trim().split(",")
						).map(s -> s.trim()).collect(ImmutableList.toImmutableList());
					}

					Matcher matcherHeadData = PATTERN_HEAD_DATA.matcher(line);
					if (matcherHeadData.matches()) {
						AtomicInteger indexer = new AtomicInteger(0);
						columns = Arrays.stream(
								line.replaceFirst("#", "").trim().split("\\s+")
						).map(s -> s.trim().toUpperCase()).filter(s -> !s.isEmpty())
								.collect(Collectors.toMap((c) -> c, (c) -> indexer.getAndIncrement()));
					}
					continue;
				} else {
					nextVariant = readVariant(buildRecord(line));
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<String> getSamples() {
		return samples;
	}

	public boolean hasNext() {
		return (nextVariant != null);
	}

	public MAVariantCNV next() throws NoSuchElementException {
		if (nextVariant == null) {
			throw new NoSuchElementException();
		}

		try {
			MAVariantCNV result = nextVariant;
			if (nextProcessedRecord != null) {
				nextVariant = readVariant(nextProcessedRecord);
			} else {
				nextVariant = null;
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private MAVariantCNV readVariant(CNVFileRecord record) throws IOException {
		List<CNVFileRecord> records = new ArrayList<>();
		records.add(record);

		while (true) {
			String line = bufferedReader.readLine();
			if (line != null) {
				CNVFileRecord iRecord = buildRecord(line);
				if (record.chromosome.equals(iRecord.chromosome) &&
						record.start == iRecord.start &&
						record.end == iRecord.end
				) {
					records.add(iRecord);
				} else {
					nextProcessedRecord = iRecord;
					break;
				}
			} else {
				nextProcessedRecord = null;
				close();
				break;
			}
		}

		LinkedHashSet<String> exonNums = new LinkedHashSet<String>();
		for (CNVFileRecord item : records) {
			exonNums.add(item.exonNum);
		}

		LinkedHashSet<String> transcripts = new LinkedHashSet<String>();
		for (CNVFileRecord item : records) {
			transcripts.add(item.transcript);
		}

		List<GenotypeCNV> genotypes = new ArrayList<>();
		for (int i = 0; i < samples.size(); i++) {
			String sample = samples.get(i);
			String gt = record.gts[i].trim();
			float lo = Float.parseFloat(record.los[i].trim());
			genotypes.add(new GenotypeCNV(sample, gt, lo));
		}

		VariantCNV variantCNV = new VariantCNV(
				record.chromosome,
				record.start, record.end,
				new ArrayList<>(exonNums),
				new ArrayList<>(transcripts),
				genotypes
		);

		return new MAVariantCNV(variantCNV);
	}

	private CNVFileRecord buildRecord(String line) {
		String[] values = Arrays.stream(
				line.split("\\s+")
		).map(s -> s.trim()).filter(s -> !s.isEmpty()).toArray(String[]::new);

		Chromosome chromosome = Chromosome.of(values[columns.get(COLUMN_CHROM)]);
		int start = Integer.parseInt(values[columns.get(COLUMN_START)]);
		int end = Integer.parseInt(values[columns.get(COLUMN_END)]);
		String exonNum = values[columns.get(COLUMN_EXON_NUM)];
		String transcript = values[columns.get(COLUMN_TRANSCRIPT)];
		String[] gts = values[columns.get(COLUMN_GT)].split(":");
		String[] los = values[columns.get(COLUMN_LO)].split(":");
		return new CNVFileRecord(
				chromosome,
				start, end,
				exonNum,
				transcript,
				gts, los
		);
	}

	@Override
	public void close() throws IOException {
		this.bufferedReader.close();
		this.inputStream.close();
	}
}
