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

import org.apache.commons.cli.*;
import org.forome.annotation.annotator.main.AnnotatorMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserArgument {

	public static final String OPTION_FILE_CONFIG = "config";

	public static final String OPTION_PATH_DATABASE = "database";

	public final Arguments arguments;

	public ParserArgument(String[] args) throws InterruptedException {
		Options options = new Options()

				.addOption(Option.builder()
						.longOpt(OPTION_FILE_CONFIG)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to config file")
						.build())

				.addOption(Option.builder()
						.longOpt(OPTION_PATH_DATABASE)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to database")
						.build())
				;

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);

			arguments = new ArgumentsMakeConservation(cmd);
		} catch (Throwable ex) {
			getLazyLogger().error("Exception: ", ex);
			new HelpFormatter().printHelp("", options);

			throw new InterruptedException();
		}
	}

	private static Logger getLazyLogger() {
		return LoggerFactory.getLogger(AnnotatorMain.class);
	}
}
