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

package org.forome.annotation.utils.vcf.convert;

import org.forome.annotation.utils.vcf.convert.main.argument.Arguments;
import org.forome.annotation.utils.vcf.convert.main.argument.ParserArgument;
import org.forome.annotation.utils.vcf.convert.struct.Record;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MainVcfConvert {

	private final static Logger log = LoggerFactory.getLogger(MainVcfConvert.class);

	private static Pattern PATTERN_CONFIG_ID = Pattern.compile(
			"^##contig=<ID=(.*)>$", Pattern.CASE_INSENSITIVE
	);

	public static void main(String[] args) {
		Arguments arguments;
		try {
			ParserArgument argumentParser = new ParserArgument(args);
			arguments = argumentParser.arguments;
		} catch (Throwable e) {
			log.error("Exception arguments parser", e);
			System.exit(2);
			return;
		}

		try {
			LiftoverConnector liftoverConnector = new LiftoverConnector();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(arguments.sourceVcf))))) {
				try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(arguments.targetVcf))) {

					Map<Chromosome, List<Record>> values = new HashMap<>();

					int count = 0;
					String line;
					while ((line = br.readLine()) != null) {
						Matcher matcherConfigID = PATTERN_CONFIG_ID.matcher(line);
						if (matcherConfigID.matches()) {
							//Добавляем информацию, что это hg19
							String out = "##contig=<ID=" + matcherConfigID.group(1) + ",assembly=hg19>";

							os.write(out.getBytes(StandardCharsets.UTF_8));
							os.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
						} else if (!line.startsWith("#")) {
							String[] parse = line.split("\t", 3);
							StringBuilder out = new StringBuilder();

							//Хромосома
							if (!Chromosome.isSupportChromosome(parse[0])) {
								continue;
							}
							out.append(parse[0]).append('\t');

							//Позиция
							Position pos19 = liftoverConnector.toHG19(new Position(
									Chromosome.of(parse[0]), Integer.parseInt(parse[1])
							));
							if (pos19 == null) {
								continue;
							}
							out.append(pos19.value).append('\t');

							//Тело
							out.append(parse[2]);

							List<Record> records = values.computeIfAbsent(pos19.chromosome, chromosome -> new ArrayList<>());
							records.add(new Record(
									pos19, out.toString()
							));
						} else {
							os.write(line.getBytes(StandardCharsets.UTF_8));
							os.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
						}

						count++;
						if (count % 10000 == 0) {
							log.debug("Progress: {}", count);
						}
					}

					write(os, values);
				}
			}
		} catch (Throwable e) {
			log.error("Exception", e);
			System.exit(2);
			return;
		}
	}

	private static void write(OutputStream os, Map<Chromosome, List<Record>> values) throws IOException {
		for (Chromosome chromosome : Chromosome.CHROMOSOMES) {
			List<Record> records = values.get(chromosome);
			if (records != null) {
				write(os, records);
			} else {
				log.debug("chromosome {} is skipped", chromosome);
			}
		}
	}

	private static void write(OutputStream os, List<Record> records) throws IOException {
		log.debug("flush({}: {})...", records.get(0).position.chromosome, records.size());

		records.sort(Comparator.comparingInt(o -> o.position.value));
		for (Record record : records) {
			os.write(record.value.getBytes(StandardCharsets.UTF_8));
			os.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
		}
		os.flush();
	}
}
