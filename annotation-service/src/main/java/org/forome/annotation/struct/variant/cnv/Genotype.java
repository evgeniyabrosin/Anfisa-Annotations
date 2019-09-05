package org.forome.annotation.struct.variant.cnv;

public class Genotype {

    public final String sampleName;

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

    public Genotype(String sampleName, String gt, float lo) {
        this.sampleName = sampleName;
        this.gt = gt;
        this.lo = lo;
    }
}
