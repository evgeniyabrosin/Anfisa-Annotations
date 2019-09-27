package org.forome.annotation.struct.variant.vcf;

import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.variant.Genotype;

public class GenotypeVCF extends Genotype {

    private final VariantContext variantContext;
    private final htsjdk.variant.variantcontext.Genotype vcfGenotype;

    public GenotypeVCF(VariantContext variantContext, String sampleName) {
        super(sampleName);
        this.variantContext = variantContext;
        this.vcfGenotype = variantContext.getGenotype(sampleName);
    }

    @Override
    public int hasVariant() {
        // REF/REF: 0;
        // REF/ALTn: 1
        // ALTn/ALTk: 2
        switch (vcfGenotype.getType()) {
            case NO_CALL: //Генотип не может быть определен из-за плохого качества секвенирования
            case UNAVAILABLE: //Не имеет альтернативных аллелей
            case MIXED:
                return 0;
            case HOM_REF:
            case HET:
            case HOM_VAR:
                //Звездочка означает мусор. Считаем, что звездочка – это референс
                String ref = variantContext.getReference().getBaseString();
                String allele1 = vcfGenotype.getAlleles().get(0).getBaseString();
                String allele2 = vcfGenotype.getAlleles().get(1).getBaseString();
                boolean isRef1 = "*".equals(allele1) || ref.equals(allele1);
                boolean isRef2 = "*".equals(allele2) || ref.equals(allele2);
                if (isRef1 && isRef2) {
                    // REF/REF
                    return 0;
                } else if (!isRef1 && !isRef2) {
                    // ALTn/ALTk
                    return 2;
                } else {
                    // REF/ALTn
                    return 1;
                }
            default:
                throw new RuntimeException("Unknown state: " + vcfGenotype.getType());
        }
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
