package org.forome.annotation.connector.gtf.struct;

public class GTFRegion {

    public static GTFRegion UPSTREAM = new GTFRegion("upstream", null);

    public static GTFRegion DOWNSTREAM = new GTFRegion("downstream", null);

    public final String region;
    public final Integer indexRegion;

    public GTFRegion(String region, Integer indexRegion) {
        this.region = region;
        this.indexRegion = indexRegion;
    }
}

