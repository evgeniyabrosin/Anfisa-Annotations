package org.forome.annotation.struct.variant;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Chromosome;

import java.util.List;

public class VariantVCF extends Variant {

    public final VariantContext variantContext;

    public VariantVCF(VariantContext variantContext) {
        super(
                Chromosome.of(variantContext.getContig()),
                getStart(variantContext),
                getEnd(variantContext)
        );
        this.variantContext = variantContext;
    }

    public static int getStart(VariantContext variantContext) {
        if (isEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
            return variantContext.getStart();
        } else {
            return variantContext.getStart() + 1;
        }
    }

    public static int getEnd(VariantContext variantContext) {
        return variantContext.getEnd();
    }

    /**
     * Проверяем, что длина одного из аллелей равна длине реверенса
     * @param reference
     * @param alleles
     * @return
     */
    private static boolean isEqualsLength(Allele reference, List<Allele> alleles) {
        for (Allele allele : alleles) {
            if (allele.length() == reference.length()) {
                return true;
            }
        }
        return false;
    }
}
