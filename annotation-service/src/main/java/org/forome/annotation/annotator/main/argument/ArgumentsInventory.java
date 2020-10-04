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

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgumentsInventory extends Arguments {

	public final Path config;

	public final Path pathInventory;

	public final int start;
	public final Path pathRecoveryAnfisaJson;

	public ArgumentsInventory(CommandLine cmd) {
		super(cmd);

		String strConfigFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_CONFIG);
		if (Strings.isNullOrEmpty(strConfigFile)) {
			throw new IllegalArgumentException("Missing config file");
		}
		config = Paths.get(strConfigFile).toAbsolutePath();
		if (!Files.exists(config)) {
			throw new IllegalArgumentException("Config file does not exists: " + config);
		}

		String strInventoryFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_INVENTORY);
		if (Strings.isNullOrEmpty(strInventoryFile)) {
			throw new IllegalArgumentException("Missing inventory file");
		}
		pathInventory = Paths.get(strInventoryFile).toAbsolutePath();
		if (!Files.exists(pathInventory)) {
			throw new IllegalArgumentException("Inventory file does not exists: " + pathInventory);
		}

		this.start = Integer.parseInt(cmd.getOptionValue(ParserArgument.OPTION_START_POSITION, "0"));

		String strRecoveryAnfisaJsonFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_RECOVERY);
		if (strRecoveryAnfisaJsonFile != null) {
			pathRecoveryAnfisaJson = Paths.get(strRecoveryAnfisaJsonFile).toAbsolutePath();
			if (!Files.exists(pathRecoveryAnfisaJson)) {
				throw new IllegalArgumentException("Recovery file does not exists: " + pathRecoveryAnfisaJson);
			}
		} else {
			pathRecoveryAnfisaJson = null;
		}

		if (start != 0 && pathRecoveryAnfisaJson != null) {
			throw new IllegalArgumentException("Conflict argument recovery file and start position");
		}
	}

}
