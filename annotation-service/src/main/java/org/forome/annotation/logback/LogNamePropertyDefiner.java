package org.forome.annotation.logback;

import ch.qos.logback.core.PropertyDefinerBase;

public class LogNamePropertyDefiner extends PropertyDefinerBase {

    public static String FILENAME = "main";

    @Override
    public String getPropertyValue() {
        return FILENAME;
    }
}
