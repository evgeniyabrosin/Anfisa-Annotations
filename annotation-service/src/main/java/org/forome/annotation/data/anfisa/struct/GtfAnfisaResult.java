/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.anfisa.struct;

import net.minidev.json.JSONArray;

import java.util.Collections;
import java.util.List;

public class GtfAnfisaResult {

    public static class RegionAndBoundary {

        public static class DistanceFromBoundary {
            public final long dist;
            public final String region;
            public final Integer index;
            public final Integer size;

            public DistanceFromBoundary(long dist, String region, Integer index, Integer size) {
                this.dist = dist;
                this.region = region;
                this.index = index;
                this.size = size;
            }

            public JSONArray toJSON() {
                JSONArray out = new JSONArray();
                out.add(dist);
                out.add(region);
                out.add(index);
                out.add(size);
                return out;
            }
        }

        public final String[] region;
        public final List<DistanceFromBoundary> distances;

        public RegionAndBoundary(String[] region, List<DistanceFromBoundary> distances) {
            this.region = region;
            this.distances = Collections.unmodifiableList(distances);
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
