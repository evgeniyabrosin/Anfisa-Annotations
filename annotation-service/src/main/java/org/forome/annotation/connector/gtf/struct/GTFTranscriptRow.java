package org.forome.annotation.connector.gtf.struct;

public class GTFTranscriptRow {

    public final String gene;
    public final int start;
    public final int end;
    public final String feature;

    public GTFTranscriptRow(String gene, int start, int end, String feature) {
        this.gene = gene;
        this.start = start;
        this.end = end;
        this.feature = feature;
    }
}
