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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public abstract class Arguments {

	private final CommandLine cmd;

	protected Arguments(CommandLine cmd) {
		this.cmd = cmd;
	}

	public String getArguments() {
		StringBuilder argumentsBuilder = new StringBuilder();
		for (int i = 0; i < cmd.getOptions().length; i++) {
			Option option = cmd.getOptions()[i];
			String key = option.getLongOpt();
			if (cmd.hasOption(key)) {
				argumentsBuilder.append(" -").append(key).append(' ').append(cmd.getOptionValue(key));
			}

			if (i < cmd.getOptions().length - 1) {
				argumentsBuilder.append(" \\\n");
			}
		}
		return argumentsBuilder.toString();
	}
}