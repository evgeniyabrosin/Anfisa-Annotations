package org.forome.annotation.struct.variant;

public abstract class Genotype {

    public final String sampleName;

    public Genotype(String sampleName) {
        this.sampleName = sampleName;
    }

    public abstract int hasVariant();

    public abstract String getGenotypeString();

}
