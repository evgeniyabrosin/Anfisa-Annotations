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

package org.forome.annotation.utils.vcf.merge;

import org.forome.annotation.utils.RuntimeExec;
import org.forome.annotation.utils.vcf.merge.main.argument.Arguments;
import org.forome.annotation.utils.vcf.merge.main.argument.ParserArgument;
import org.forome.annotation.utils.vcf.merge.main.interactive.Interactive;
import org.forome.annotation.utils.vcf.merge.struct.BgzipFile;
import org.forome.annotation.utils.vcf.merge.struct.Patient;
import org.forome.annotation.utils.vcf.merge.struct.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainVcfMerge {

	private final static Logger log = LoggerFactory.getLogger(MainVcfMerge.class);

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

		Interactive interative = new Interactive();
		try {
			Map<String, Patient> patients = getPatients(arguments.patientIdsFile);
			List<Source> sourceFiles = getSourceFiles(interative, patients, arguments.pathSourceVcfs);
			List<BgzipFile> reheaderVcfFiles = reheaderVcfFiles(sourceFiles);
			merge(reheaderVcfFiles, arguments.targetVcf);
			clear(reheaderVcfFiles);
			log.debug("Complete");
		} catch (Throwable e) {
			log.error("Exception merge vcf files", e);
			System.exit(4);
			return;
		}
	}

	private static Map<String, Patient> getPatients(Path patientIdsFile) throws IOException {
		Map<String, Patient> patients = new HashMap<>();
		try (BufferedReader isBFamSampleName = new BufferedReader(new InputStreamReader(Files.newInputStream(patientIdsFile)))) {
			String line;
			while ((line = isBFamSampleName.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) continue;

				String[] values = Arrays.stream(line.split("\t")).filter(s -> !s.trim().isEmpty()).toArray(String[]::new);
				if (values.length != 4) {
					throw new RuntimeException("Not support FamSampleName format");
				}

				Patient patient = new Patient(
						values[0].trim(), values[1].trim(),
						values[3].trim()
				);

				if (patients.values().stream()
						.filter(iPatient -> iPatient.targetPatientId.equals(patient.targetPatientId))
						.count() > 0
				) {
					throw new RuntimeException("Duplicate: " + patient.targetPatientId);
				}
				patients.put(patient.getSourcePatientId(), patient);
			}
		}
		return patients;
	}


	private static List<Source> getSourceFiles(Interactive interative, Map<String, Patient> patients, Path pathSourceVcfs) throws IOException {
		List<Source> sources;
		try (Stream<Path> paths = Files.walk(pathSourceVcfs)) {
			sources = paths
					.filter(Files::isRegularFile)
					.filter(path -> !path.getFileName().endsWith(".vcf"))
					.map(path -> {
						String vcfPatientId = null;
						try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
							String line;
							while ((line = br.readLine()) != null) {
								if (!line.startsWith("#CHROM")) continue;
								String[] values = Arrays.stream(line.split("\t")).filter(s -> !s.trim().isEmpty()).toArray(String[]::new);
								if (values.length != 10) {
									throw new RuntimeException("Not support vcf file");
								}
								vcfPatientId = values[9];
								break;
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}

						Patient patient = patients.get(vcfPatientId);
						if (patient == null) {
							String question = "Sample: " + vcfPatientId + " not found. Skipped?";
							boolean skipped = interative.questionBool(question);
							if (skipped) {
								return null;
							} else {
								System.exit(3);
							}
						}

						return new Source(patient, path.toAbsolutePath());
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		}

		if (sources.size() != patients.size()) {
			Set<Patient> leftover = new HashSet<>(patients.values());
			leftover.removeAll(sources.stream().map(source -> source.patient).collect(Collectors.toList()));
			log.debug("Not found: {}", leftover);
			System.exit(5);
		}

		return sources;
	}

	private static List<BgzipFile> reheaderVcfFiles(List<Source> sourceFiles) throws IOException {
		List<BgzipFile> results = new ArrayList<>();

		Path tmpFileWithPatientName = Files.createTempFile("merge_vcf_file", ".txt").toAbsolutePath();
		for (Source source : sourceFiles) {
			Files.write(
					tmpFileWithPatientName,
					source.patient.targetPatientId.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE
			);

			Path tmpVcfFile = Files.createTempFile(source.patient.getSourcePatientId(), ".vcf").toAbsolutePath();
			log.debug("reheader: {}", tmpVcfFile.toString());

			//Меняем хедер
			String cmd = new StringBuilder()
					.append("bcftools reheader -s")
					.append(tmpFileWithPatientName.toString())
					.append(" ")
					.append(source.pathSourceVcf)
					.append(" -o ")
					.append(tmpVcfFile)
					.toString();
			exec(cmd);

			//Упаковываем
			exec("bgzip " + tmpVcfFile.toString());
			Path tmpVcfFileGZ = tmpVcfFile.getParent().resolve(tmpVcfFile.getFileName() + ".gz").toAbsolutePath();

			//Создаем индекс
			exec("tabix " + tmpVcfFileGZ.toString());
			Path tmpVcfFileGZIndex = tmpVcfFileGZ.getParent().resolve(tmpVcfFileGZ.getFileName() + ".tbi").toAbsolutePath();

			results.add(new BgzipFile(tmpVcfFileGZ, tmpVcfFileGZIndex));
		}
		Files.deleteIfExists(tmpFileWithPatientName);

		return results;
	}

	private static void merge(List<BgzipFile> reheaderVcfFiles, Path target) {
		String cmd = new StringBuilder()
				.append("bcftools merge -m all ")
				.append(reheaderVcfFiles.stream()
						.map(bgzipFile -> bgzipFile.file.toString())
						.collect(Collectors.joining(" "))
				)
				.append(" -o ")
				.append(target.toString())
				.append(" -O v --threads 8")
				.toString();

		log.debug("Merge files: {}", cmd);
		exec(cmd);
	}

	private static void clear(List<BgzipFile> reheaderVcfFiles) throws IOException {
		for (BgzipFile bgzipFile : reheaderVcfFiles) {
			log.debug("Clear: {}", bgzipFile.file);
			Files.deleteIfExists(bgzipFile.file);

			log.debug("Clear: {}", bgzipFile.fileIndex);
			Files.deleteIfExists(bgzipFile.fileIndex);
		}
	}

	private static void exec(String cmd) {
		RuntimeExec.Result result;
		try {
			result = RuntimeExec.runCommand(cmd);
		} catch (Exception e) {
			throw new RuntimeException("Exception exec cmd: " + cmd, e);
		}
		if (result.exitCode != 0) {
			throw new RuntimeException("Exception exec cmd, return code: '" + result.exitCode + "' out: '" + result.out + "', error out: " + result.outError);
		}
	}
}
