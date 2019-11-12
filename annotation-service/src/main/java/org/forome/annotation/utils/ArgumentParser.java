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

package org.forome.annotation.utils;

import org.apache.commons.cli.*;

public class ArgumentParser {

	private static final String OPTION_PORT = "port";

	private static final int DEFAULT_PORT = 8095;

	public final int port;

	public ArgumentParser(String[] args) throws InterruptedException {
		Options options = new Options()
				.addOption(Option.builder()
						.longOpt(OPTION_PORT)
						.hasArg(true)
						.optionalArg(true)
						.desc("Absolute path to data directory")
						.build());

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);

			port = Integer.parseInt(cmd.getOptionValue(OPTION_PORT, String.valueOf(DEFAULT_PORT)));
		} catch (ParseException | IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
			new HelpFormatter().printHelp("", options);

			throw new InterruptedException();
		}
	}
}
