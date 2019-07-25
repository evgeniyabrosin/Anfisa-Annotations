package org.forome.annotation.logback;

import java.nio.file.Path;

public class LogbackConfigure {

    public static void setLogPath(Path logFile) {
        LogDirPropertyDefiner.LOG_DIR = logFile.toAbsolutePath().getParent().toString();

        String logFileName = logFile.toAbsolutePath().getFileName().toString();
        logFileName = logFileName.substring(0, logFileName.lastIndexOf('.'));
        LogNamePropertyDefiner.FILENAME = logFileName;
    }
}
