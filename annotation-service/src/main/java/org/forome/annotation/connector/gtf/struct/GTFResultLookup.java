package org.forome.annotation.connector.gtf.struct;

public class GTFResultLookup {

    public final String transcript;
    public final String gene;
    public final long position;
    public final String region;
    public final Integer index;

    public GTFResultLookup(String transcript, String gene, long position, String region, Integer index) {
        this.transcript = transcript;
        this.gene = gene;
        this.position = position;
        this.region = region;
        this.index = index;
    }
}
