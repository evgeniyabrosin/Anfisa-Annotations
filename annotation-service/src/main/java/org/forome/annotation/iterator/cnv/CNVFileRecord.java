package org.forome.annotation.iterator.cnv;

import org.forome.annotation.struct.Chromosome;

public class CNVFileRecord {

    protected final Chromosome chromosome;
    protected final int start;
    protected final int end;
    protected final String[] gts;
    protected final String[] los;

    protected CNVFileRecord(String value) {
        String[] values = value.split("\t");

        chromosome = Chromosome.of(values[0]);
        start = Integer.parseInt(values[1]);
        end = Integer.parseInt(values[2]);

        gts = values[6].split(":");
        los = values[7].split(":");
    }


}
