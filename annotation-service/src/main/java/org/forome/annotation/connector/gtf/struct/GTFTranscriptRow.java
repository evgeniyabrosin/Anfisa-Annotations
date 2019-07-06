package org.forome.annotation.connector.gtf.struct;

public class GTFTranscriptRow {

    public final String gene;
    public final long start;
    public final long end;
    public final String feature;

    public GTFTranscriptRow(String gene, long start, long end, String feature) {
        this.gene = gene;
        this.start = start;
        this.end = end;
        this.feature = feature;
    }
}
