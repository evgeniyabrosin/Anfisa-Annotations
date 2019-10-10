package org.forome.annotation.connector.anfisa.struct;

import java.util.Collections;
import java.util.List;

public class GtfAnfisaResult {

    public static class RegionAndBoundary {

        public final String region;
        public final List<Object[]> distFromBoundary;

        public RegionAndBoundary(String region, List<Object[]> distFromBoundary) {
            this.region = region;
            this.distFromBoundary = Collections.unmodifiableList(distFromBoundary);
        }
    }

    public final RegionAndBoundary canonical;
    public final RegionAndBoundary worst;

    public GtfAnfisaResult(RegionAndBoundary canonical, RegionAndBoundary worst) {
        this.canonical = canonical;
        this.worst = worst;
    }

    public RegionAndBoundary getRegion(Kind kind) {
        if (kind == Kind.CANONICAL) {
            return canonical;
        } else if (kind == Kind.WORST) {
            return worst;
        } else {
            throw new RuntimeException("Unknown kind: " + kind);
        }
    }
}
