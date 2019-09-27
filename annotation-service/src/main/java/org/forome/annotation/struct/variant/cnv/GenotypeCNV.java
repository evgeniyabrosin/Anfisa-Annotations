package org.forome.annotation.struct.variant.cnv;

import org.forome.annotation.struct.variant.Genotype;

import java.util.Arrays;

public class GenotypeCNV extends Genotype {

    private VariantCNV variantCNV;

    /**
     * 0/0 – означает, что сэмпл гомозиготен по референсу (то есть, у него вариант отсутствует),
     * 0/1 – это гетерозигота, то есть, имеется один альтернативный аллель,
     * 1/1 – гомозигота, то есть, оба аллеля – альтернативные
     */
    public final String gt;

    /**
     * Оценка надежности данной записи
     */
    public final float lo;

    public GenotypeCNV(String sampleName, String gt, float lo) {
        super(sampleName);
        this.gt = gt;
        this.lo = lo;
    }

    protected void setVariantCNV(VariantCNV variantCNV) {
        this.variantCNV = variantCNV;
    }

    @Override
    public int hasVariant() {
        // REF/REF: 0;
        // REF/ALTn: 1
        // ALTn/ALTk: 2
        int[] gts = Arrays.stream(gt.split("/")).map(s -> Integer.parseInt(s)).mapToInt(Integer::intValue).toArray();
        if (gts[0] == 0 && gts[1] == 0) {
            return 0;
        } else if (gts[0] == 0 || gts[1] == 0) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    public String getGenotypeString() {
        int[] gts = Arrays.stream(gt.split("/")).map(s -> Integer.parseInt(s)).mapToInt(Integer::intValue).toArray();
        return variantCNV.getAllele(gts[0]) + "/" + variantCNV.getAllele(gts[1]);
    }

}
