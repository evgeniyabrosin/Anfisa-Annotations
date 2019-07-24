package org.forome.annotation.annotator.main.argument;

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