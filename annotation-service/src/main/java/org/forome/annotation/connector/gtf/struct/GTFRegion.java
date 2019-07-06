package org.forome.annotation.connector.gtf.struct;

public class GTFRegion {

    public static GTFRegion UPSTREAM = new GTFRegion("upstream", null);

    public static GTFRegion DOWNSTREAM = new GTFRegion("downstream", null);

    public final String region;
    public final Long indexRegion;

    public GTFRegion(String region, Long indexRegion) {
        this.region = region;
        this.indexRegion = indexRegion;
    }
}

