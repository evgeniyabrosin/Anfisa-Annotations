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

package org.forome.annotation.makefavordatabase.validation.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Arguments {

	public final Path database;

	public Arguments(CommandLine cmd) {
		String strSourceFile = cmd.getOptionValue(ParserArgument.OPTION_DATABASE);
		if (Strings.isNullOrEmpty(strSourceFile)) {
			throw new IllegalArgumentException("Missing database");
		}
		database = Paths.get(strSourceFile).toAbsolutePath();
		if (!Files.exists(database)) {
			throw new IllegalArgumentException("Database is not exists: " + database);
		}
	}

}
