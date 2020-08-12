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

package org.forome.annotation.utils.vcf.merge.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Arguments {

	public final Path patientIdsFile;
	public final Path pathSourceVcfs;

	public final Path targetVcf;

	public Arguments(CommandLine cmd) {

		String strPatientIdsFile = cmd.getOptionValue(ParserArgument.OPTION_SAMPLES);
		if (Strings.isNullOrEmpty(strPatientIdsFile)) {
			throw new IllegalArgumentException("Missing samples file");
		}
		patientIdsFile = Paths.get(strPatientIdsFile).toAbsolutePath();
		if (!Files.exists(patientIdsFile)) {
			throw new IllegalArgumentException("Samples file is not exists: " + patientIdsFile);
		}
		if (!patientIdsFile.getFileName().toString().endsWith(".csv")) {
			throw new IllegalArgumentException("Bad source file: " + patientIdsFile);
		}


		String strSourceFile = cmd.getOptionValue(ParserArgument.OPTION_SOURCE);
		if (Strings.isNullOrEmpty(strSourceFile)) {
			throw new IllegalArgumentException("Missing source file");
		}
		pathSourceVcfs = Paths.get(strSourceFile).toAbsolutePath();
		if (!Files.exists(pathSourceVcfs)) {
			throw new IllegalArgumentException("Path source vcf's is not exists: " + pathSourceVcfs);
		}
		if (!Files.isDirectory(pathSourceVcfs)) {
			throw new IllegalArgumentException("Path source vcf's is not directory: " + pathSourceVcfs);
		}


		String strTargetFile = cmd.getOptionValue(ParserArgument.OPTION_TARGET);
		if (Strings.isNullOrEmpty(strTargetFile)) {
			throw new IllegalArgumentException("Missing target file");
		}
		targetVcf = Paths.get(strTargetFile).toAbsolutePath();
		if (!targetVcf.getFileName().toString().endsWith(".vcf")) {
			throw new IllegalArgumentException("Bad target file: " + targetVcf.toAbsolutePath());
		}
	}

}
