package org.forome.annotation.struct;

public class Allele {

    public static Allele EMPTY = new Allele(null);

    public static String EMPTY_BASE = "-";

    private final String bases;

    public Allele(String bases) {
        this.bases = bases;
    }

    public String getBaseString() {
        if (bases == null) {
            return EMPTY_BASE;
        } else {
            return bases;
        }
    }
}
