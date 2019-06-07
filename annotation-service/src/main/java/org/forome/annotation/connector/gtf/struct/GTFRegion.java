package org.forome.annotation.connector.gtf.struct;

public class GTFRegion {

    public static GTFRegion UPSTREAM = new GTFRegion("upstream", null);

    public static GTFRegion DOWNSTREAM = new GTFRegion("downstream", null);

    public final String typeRegion;
    public final Long indexRegion;

    public GTFRegion(String typeRegion, Long indexRegion) {
        this.typeRegion = typeRegion;
        this.indexRegion = indexRegion;
    }
}

