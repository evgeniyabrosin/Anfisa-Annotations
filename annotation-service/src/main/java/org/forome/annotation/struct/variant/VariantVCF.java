package org.forome.annotation.struct.variant;

import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Chromosome;

public class VariantVCF extends Variant {

    public final VariantContext variantContext;

    public VariantVCF(VariantContext variantContext, int start, int end) {
        super(
                new Chromosome(variantContext.getContig()),
                start, //getStart(variantContext),
                end //variantContext.getEnd()
        );
        this.variantContext = variantContext;
    }

    public static int getStart(VariantContext variantContext) {
        throw new RuntimeException("Not implemented");
        //Для определения start. Совершенно нет стандартов, поэтому пока был выбран подход как у vep
//        if (!variantContext.isSimpleInsertion() && !variantContext.isSimpleDeletion()) {
//            return variantContext.getStart();
//        } else if (!variantContext.isSimpleInsertion() && variantContext.isSimpleDeletion()) {
//            return variantContext.getStart() + 1;
//        } else {
//            throw new RuntimeException("Unknown");
//        }
    }
}
