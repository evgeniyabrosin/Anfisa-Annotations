/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.makedatabase.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.forome.annotation.struct.Assembly;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgumentsMake extends Arguments {

	public final Path config;

	public final Path database;

	public final Assembly assembly;

	public ArgumentsMake(CommandLine cmd) {
		super(cmd);

		String strConfigFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_CONFIG);
		if (Strings.isNullOrEmpty(strConfigFile)) {
			throw new IllegalArgumentException("Missing config file");
		}
		config = Paths.get(strConfigFile).toAbsolutePath();
		if (!Files.exists(config)) {
			throw new IllegalArgumentException("Config file is not exists: " + config);
		}

		String strPathDatabase = cmd.getOptionValue(ParserArgument.OPTION_PATH_DATABASE);
		if (Strings.isNullOrEmpty(strPathDatabase)) {
			throw new IllegalArgumentException("Missing path database");
		}
		database = Paths.get(strPathDatabase).toAbsolutePath();
		if (Files.exists(database) && !Files.isDirectory(database)) {
			throw new IllegalArgumentException("path database is file: " + config);
		}

		String strAssembly = cmd.getOptionValue(ParserArgument.OPTION_ASSEMBLY);
		if (Strings.isNullOrEmpty(strPathDatabase)) {
			throw new IllegalArgumentException("Missing assembly");
		}
		assembly = Assembly.valueOf(strAssembly);
	}
}
