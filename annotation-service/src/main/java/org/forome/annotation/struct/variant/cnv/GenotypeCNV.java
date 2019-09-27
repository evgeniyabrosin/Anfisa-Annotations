package org.forome.annotation.struct.variant.cnv;

import org.forome.annotation.struct.variant.Genotype;

public class GenotypeCNV extends Genotype {

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

    @Override
    public String getGenotypeString() {
        return null;
    }
}
