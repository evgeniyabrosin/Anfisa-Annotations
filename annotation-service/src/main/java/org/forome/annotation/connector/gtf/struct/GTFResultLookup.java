package org.forome.annotation.connector.gtf.struct;

public class GTFResultLookup {

    public final String transcript;
    public final long position;
    public final String region;
    public final Long index;

    public GTFResultLookup(String transcript, long position, String region, Long index) {
        this.transcript = transcript;
        this.position = position;
        this.region = region;
        this.index = index;
    }
}
