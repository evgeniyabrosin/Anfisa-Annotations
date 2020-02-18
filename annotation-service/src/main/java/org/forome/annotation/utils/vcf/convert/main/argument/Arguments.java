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

package org.forome.annotation.utils.vcf.convert.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Arguments {



	public final Path sourceVcf;

	public final Path targetVcf;

	public Arguments(CommandLine cmd) {

		String strSourceFile = cmd.getOptionValue(ParserArgument.OPTION_SOURCE);
		if (Strings.isNullOrEmpty(strSourceFile)) {
			throw new IllegalArgumentException("Missing source file");
		}
		sourceVcf = Paths.get(strSourceFile).toAbsolutePath();
		if (!Files.exists(sourceVcf)) {
			throw new IllegalArgumentException("Source file is not exists: " + sourceVcf);
		}
		if (!sourceVcf.getFileName().toString().endsWith(".vcf.gz")) {
			throw new IllegalArgumentException("Bad source file: " + sourceVcf.toAbsolutePath());
		}

		String strTargetFile = cmd.getOptionValue(ParserArgument.OPTION_TARGET);
		if (Strings.isNullOrEmpty(strTargetFile)) {
			throw new IllegalArgumentException("Missing target file");
		}
		targetVcf = Paths.get(strTargetFile).toAbsolutePath();
		if (!targetVcf.getFileName().toString().endsWith(".vcf.gz")) {
			throw new IllegalArgumentException("Bad target file: " + sourceVcf.toAbsolutePath());
		}
	}

}
