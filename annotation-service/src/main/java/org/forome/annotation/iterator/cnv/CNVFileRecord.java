package org.forome.annotation.iterator.cnv;

import org.forome.annotation.struct.Chromosome;

public class CNVFileRecord {

    protected final Chromosome chromosome;
    protected final int start;
    protected final int end;
    protected final String exonNum;
    protected final String transcript;
    protected final String[] gts;
    protected final String[] los;

    public CNVFileRecord(
            Chromosome chromosome,
            int start, int end,
            String exonNum,
            String transcript,
            String[] gts, String[] los
    ) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.exonNum = exonNum;
        this.transcript = transcript;
        this.gts = gts;
        this.los = los;
    }
}
