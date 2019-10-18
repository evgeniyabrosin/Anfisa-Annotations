package org.forome.annotation.struct.variant;

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Variant {

    public final Chromosome chromosome;
    public final int start;
    public final int end;

    public Variant(Chromosome chromosome, int start, int end) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
    }

    public abstract VariantType getVariantType();

    public abstract Genotype getGenotype(String sample);

    public abstract String getId();

    public abstract String getRef();

    public abstract List<Allele> getAltAllele();

    public List<String> getStrAltAllele() {
        return getAltAllele().stream().map(Allele::getBaseString).collect(Collectors.toList());
    }

    public abstract String getMostSevereConsequence();

}
