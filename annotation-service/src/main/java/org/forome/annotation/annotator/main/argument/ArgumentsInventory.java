package org.forome.annotation.annotator.main.argument;

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgumentsInventory extends Arguments {

    public final Path config;

    public final Path pathInventory;

    public ArgumentsInventory(CommandLine cmd) {
        super(cmd);

        String strConfigFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_CONFIG);
        if (Strings.isNullOrEmpty(strConfigFile)) {
            throw new IllegalArgumentException("Missing config file");
        }
        config = Paths.get(strConfigFile).toAbsolutePath();
        if (!Files.exists(config)) {
            throw new IllegalArgumentException("Config file is not exists: " + config);
        }

        String strInventoryFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_INVENTORY);
        if (Strings.isNullOrEmpty(strInventoryFile)) {
            throw new IllegalArgumentException("Missing inventory file");
        }
        pathInventory = Paths.get(strInventoryFile).toAbsolutePath();
        if (!Files.exists(pathInventory)) {
            throw new IllegalArgumentException("Inventory file is not exists: " + config);
        }
    }

}
