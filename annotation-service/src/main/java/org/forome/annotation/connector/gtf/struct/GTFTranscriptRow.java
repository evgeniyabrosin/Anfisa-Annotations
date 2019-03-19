package org.forome.annotation.connector.gtf.struct;

public class GTFTranscriptRow {

    public final long start;
    public final long end;
    public final String feature;

    public GTFTranscriptRow(long start, long end, String feature) {
        this.start = start;
        this.end = end;
        this.feature = feature;
    }
}
