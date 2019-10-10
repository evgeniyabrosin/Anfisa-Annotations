package org.forome.annotation.connector.anfisa.struct;

public enum Kind {

    CANONICAL("canonical"),

    WORST("worst");

    public final String value;

    Kind(String value) {
        this.value = value;
    }
}
