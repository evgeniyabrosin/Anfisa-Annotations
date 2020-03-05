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

package org.forome.annotation.makefavordatabase.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Arguments {

	public final Path sourceDump;

	public final Path target;

	public final int offset;
	public final int limit;

	public Arguments(CommandLine cmd) {

		String strSourceFile = cmd.getOptionValue(ParserArgument.OPTION_SOURCE);
		if (Strings.isNullOrEmpty(strSourceFile)) {
			throw new IllegalArgumentException("Missing source file");
		}
		sourceDump = Paths.get(strSourceFile).toAbsolutePath();
		if (!Files.exists(sourceDump)) {
			throw new IllegalArgumentException("Source file is not exists: " + sourceDump);
		}
		if (!sourceDump.getFileName().toString().endsWith(".gz")) {
			throw new IllegalArgumentException("Bad source file: " + sourceDump.toAbsolutePath());
		}

		String strTargetFile = cmd.getOptionValue(ParserArgument.OPTION_TARGET);
		if (Strings.isNullOrEmpty(strTargetFile)) {
			throw new IllegalArgumentException("Missing target directory");
		}
		target = Paths.get(strTargetFile).toAbsolutePath();

		this.offset = Integer.parseInt(cmd.getOptionValue(ParserArgument.OPTION_OFFSET, "0"));
		this.limit = Integer.parseInt(cmd.getOptionValue(ParserArgument.OPTION_LIMIT, String.valueOf(Integer.MAX_VALUE)));
	}

}
