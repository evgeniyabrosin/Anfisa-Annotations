package org.forome.annotation.struct.variant;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Chromosome;

import java.util.List;

public class VariantVCF extends Variant {

    public final VariantContext variantContext;

    public VariantVCF(VariantContext variantContext) {
        super(
                new Chromosome(variantContext.getContig()),
                getStart(variantContext),
                getEnd(variantContext)
        );
        this.variantContext = variantContext;
    }

    public static int getStart(VariantContext variantContext) {
        if (variantContext.getReference().length() == 1 &&
                isSingleAlleles(variantContext.getAlternateAlleles())
        ) {
            return variantContext.getStart();
        } else {
            return variantContext.getStart() + 1;
        }
    }

    public static int getEnd(VariantContext variantContext) {
        return variantContext.getEnd();
    }

    private static boolean isSingleAlleles(List<Allele> alleles) {
        for (Allele allele : alleles) {
            if (allele.length() != 1) {
                return false;
            }
        }
        return true;
    }
}
