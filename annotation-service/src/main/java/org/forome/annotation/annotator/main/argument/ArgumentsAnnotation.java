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

package org.forome.annotation.annotator.main.argument;

import org.apache.commons.cli.CommandLine;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.CasePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ArgumentsAnnotation extends Arguments {

	private final static Logger log = LoggerFactory.getLogger(ArgumentsAnnotation.class);

	public final Path config;

	public final String caseName;

	public final Assembly assembly;
	public final CasePlatform casePlatform;

	public final Path pathFam;
	public final Path patientIdsFile;
	public final Path cohortFile;
	public final Path pathVcf;
	public final Path pathVepJson;
	public final Path pathCnv;
	public final Path pathOutput;

	public final int start;
	public final Path pathRecoveryAnfisaJson;

	public ArgumentsAnnotation(CommandLine cmd) {
		super(cmd);

		config = Paths.get(cmd.getOptionValue(ParserArgument.OPTION_FILE_CONFIG));

		Path dir = Paths.get("").toAbsolutePath();

		String strCaseName = cmd.getOptionValue(ParserArgument.OPTION_CASE_NAME);
		if (strCaseName != null) {
			caseName = strCaseName;
		} else {
			caseName = dir.getFileName().toString();
		}

		assembly = Assembly.valueOf(cmd.getOptionValue(ParserArgument.OPTION_ASSEMBLY));

		String strPathFamFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_FAM);
		if (strPathFamFile != null) {
			pathFam = Paths.get(strPathFamFile).toAbsolutePath();
		} else {
			pathFam = dir.resolve(String.format("%s.fam", caseName)).toAbsolutePath();
		}

		String strPathFamSampleName = cmd.getOptionValue(ParserArgument.OPTION_FILE_FAM_NAME);
		if (strPathFamSampleName != null) {
			patientIdsFile = Paths.get(strPathFamSampleName).toAbsolutePath();
		} else {
			patientIdsFile = null;
		}

		String strCohortFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_COHORT);
		if (strCohortFile != null) {
			cohortFile = Paths.get(strCohortFile).toAbsolutePath();
		} else {
			cohortFile = null;
		}

		String strPathVepJson = cmd.getOptionValue(ParserArgument.OPTION_FILE_VEP_JSON);
		if (strPathVepJson != null) {
			this.pathVepJson = Paths.get(strPathVepJson).toAbsolutePath();
		} else {
			this.pathVepJson = null;
		}

		String strPathVcf = cmd.getOptionValue(ParserArgument.OPTION_FILE_VCF);
		if (strPathVcf != null) {
			this.pathVcf = Paths.get(strPathVcf).toAbsolutePath();
		} else {
			if (this.pathVepJson == null) {
				throw new IllegalArgumentException("Missing vcf file");
			}
			strPathVcf = this.pathVepJson.getFileName().toString().split("\\.")[0] + ".vcf";
			this.pathVcf = Paths.get(strPathVcf).toAbsolutePath();
		}

		String strPathCnv = cmd.getOptionValue(ParserArgument.OPTION_FILE_CNV);
		if (strPathCnv != null) {
			this.pathCnv = Paths.get(strPathCnv).toAbsolutePath();
		} else {
			this.pathCnv = null;
		}

		this.start = Integer.parseInt(cmd.getOptionValue(ParserArgument.OPTION_START_POSITION, "0"));

		this.pathOutput = Paths.get(cmd.getOptionValue(ParserArgument.OPTION_FILE_OUTPUT)).toAbsolutePath();

		Set<String> x = Arrays.stream(pathVcf.getFileName().toString().toLowerCase().split("_"))
				.collect(Collectors.toSet());
		if (x.contains("wgs")) {
			casePlatform = CasePlatform.WGS;
		} else if (x.contains("wes")) {
			casePlatform = CasePlatform.WES;
		} else {
			casePlatform = CasePlatform.WGS;
			log.warn("Could not determine platform (WES or WGS), assuming: " + casePlatform);
		}

		String strRecoveryAnfisaJsonFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_RECOVERY);
		if (strRecoveryAnfisaJsonFile != null) {
			pathRecoveryAnfisaJson = Paths.get(strRecoveryAnfisaJsonFile).toAbsolutePath();
			if (!Files.exists(pathRecoveryAnfisaJson)) {
				throw new IllegalArgumentException("Recovery file is not exists: " + pathRecoveryAnfisaJson);
			}
		} else {
			pathRecoveryAnfisaJson = null;
		}

		if (start != 0 && pathRecoveryAnfisaJson != null) {
			throw new IllegalArgumentException("Conflict argument recovery file and start position");
		}
	}

}
