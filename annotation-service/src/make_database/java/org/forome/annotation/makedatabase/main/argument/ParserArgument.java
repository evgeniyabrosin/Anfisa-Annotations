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

	public static final String OPTION_PATH_DATABASE = "database";

	public static final String OPTION_ASSEMBLY = "assembly";

	public static final String OPTION_SOURCE_GERP_HG19 = "gerp19";
	public static final String OPTION_SOURCE_GERP_HG38 = "gerp38";

	public final Arguments arguments;

	public ParserArgument(String[] args) throws InterruptedException {
		Options options = new Options()

				.addOption(Option.builder()
						.longOpt(OPTION_PATH_DATABASE)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to database")
						.build())

				.addOption(Option.builder()
						.longOpt(OPTION_ASSEMBLY)
						.hasArg(true)
						.optionalArg(false)
						.desc("Type assembly")
						.build())

				.addOption(Option.builder()
						.longOpt(OPTION_SOURCE_GERP_HG19)
						.hasArg(true)
						.optionalArg(true)
						.desc("Absolute path to data file: gerp hg19")
						.build())
				.addOption(Option.builder()
						.longOpt(OPTION_SOURCE_GERP_HG38)
						.hasArg(true)
						.optionalArg(true)
						.desc("Absolute path to data file: gerp hg38")
						.build())

				;

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);

			arguments = new ArgumentsMake(cmd);
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
