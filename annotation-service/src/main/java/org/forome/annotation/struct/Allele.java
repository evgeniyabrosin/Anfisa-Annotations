package org.forome.annotation.struct;

import java.util.Objects;

public class Allele {

    public static Allele EMPTY = new Allele(null);

    private static String EMPTY_BASE = "-";

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Allele allele = (Allele) o;
        return Objects.equals(bases, allele.bases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bases);
    }
}
