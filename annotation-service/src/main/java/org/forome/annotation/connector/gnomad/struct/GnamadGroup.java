package org.forome.annotation.connector.gnomad.struct;

import java.util.Arrays;

public enum GnamadGroup {

    AFR(Type.GENERAL),
    AMR(Type.GENERAL),
    EAS(Type.GENERAL),
    NFE(Type.GENERAL),
    SAS(Type.GENERAL),//South Asia

    ASJ(Type.THOROUGHBRED),
    FIN(Type.THOROUGHBRED),
    OTH(Type.THOROUGHBRED),
    RAW(Type.THOROUGHBRED);

    public enum Type {
        GENERAL,
        THOROUGHBRED
    }

    public final Type type;

    GnamadGroup(Type type) {
        this.type = type;
    }

    public static GnamadGroup[] getByType(Type type) {
        return Arrays.stream(GnamadGroup.values())
                .filter(gnamadGroup -> gnamadGroup.type == type)
                .toArray(value -> new GnamadGroup[value]);
    }
}
