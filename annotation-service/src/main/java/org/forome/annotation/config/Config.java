package org.forome.annotation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    public final Path dataPath;

    public Config() throws IOException {
        dataPath = Paths.get("data");
        if (!Files.exists(dataPath)) {
            Files.createDirectory(dataPath);
        }
    }
}
