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

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserArgument {

	private final static Logger log = LoggerFactory.getLogger(ParserArgument.class);

	public static final String OPTION_DATABASE = "database";

	public final Arguments arguments;

	public ParserArgument(String[] args) throws InterruptedException {
		Options options = new Options()
				.addOption(Option.builder()
						.longOpt(OPTION_DATABASE)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to database")
						.build());

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			arguments = new Arguments(cmd);
		} catch (Throwable ex) {
			log.error("Exception: ", ex);
			new HelpFormatter().printHelp("", options);

			throw new InterruptedException();
		}
	}
}
