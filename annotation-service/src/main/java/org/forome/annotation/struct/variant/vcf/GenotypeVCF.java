package org.forome.annotation.struct.variant.vcf;

import org.forome.annotation.struct.variant.Genotype;

public class GenotypeVCF extends Genotype {

    htsjdk.variant.variantcontext.Genotype vcfGenotype;

    public GenotypeVCF(String sampleName, htsjdk.variant.variantcontext.Genotype genotype) {
        super(sampleName);
        this.vcfGenotype = genotype;
    }

    @Override
    public String getGenotypeString() {
        if (vcfGenotype.isCalled()) {
            return vcfGenotype.getGenotypeString();
        } else {
            return null;
        }
    }
}
