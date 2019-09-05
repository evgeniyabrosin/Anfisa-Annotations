package org.forome.annotation.struct.variant.cnv;

import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariantCNV extends Variant {

    public final Map<String, Genotype> genotypes;

    public VariantCNV(Chromosome chromosome, int start, int end, List<Genotype> genotypes) {
        super(chromosome, start, end);
        this.genotypes = Collections.unmodifiableMap(
                genotypes.stream()
                        .collect(Collectors.toMap(item -> item.sampleName, item -> item))
        );
    }

    public Genotype getGenotype(String sample) {
        return genotypes.get(sample);
    }
}
