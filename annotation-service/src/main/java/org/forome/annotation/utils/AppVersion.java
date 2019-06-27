package org.forome.annotation.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AppVersion {

    private static String _hashVersion = null;

    public static String getVersion() {
        if (_hashVersion == null) {
            String version = AppVersion.class.getPackage().getImplementationVersion();
            if (version == null) {
                //Возможно это запуск из проекта
                Path fileBuildGradle = Paths.get("build.gradle");
                if (Files.exists(fileBuildGradle)) {
                    try {
                        try (Stream<String> stream = Files.lines(fileBuildGradle)) {
                            String lineWithVersion = stream.filter(s -> s.trim().startsWith("version ")).findFirst().orElse(null);
                            if (lineWithVersion != null) {
                                Pattern pattern = Pattern.compile("'(.+?)'");
                                Matcher matcher = pattern.matcher(lineWithVersion);
                                matcher.find();
                                version = matcher.group(1);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Exception find version", e);
                    }
                }
            }
            if (version == null) throw new RuntimeException("Not found version app");

            _hashVersion = version;
        }
        return _hashVersion;
    }

}
