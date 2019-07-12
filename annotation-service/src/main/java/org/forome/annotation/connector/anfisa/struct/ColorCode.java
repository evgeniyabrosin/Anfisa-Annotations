package org.forome.annotation.connector.anfisa.struct;

public enum ColorCode {

    GREEN("green"),

    GREY("grey"),

    RED_CROSS("red-cross"),

    RED("red"),

    YELLOW_CROSS("yellow-cross"),

    YELLOW("yellow");

    public final String code;

    ColorCode(String code) {
        this.code = code;
    }
}
