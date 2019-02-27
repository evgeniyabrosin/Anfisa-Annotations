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
