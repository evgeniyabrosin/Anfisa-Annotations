package org.forome.annotation.struct.sample;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public class Samples {

    public final Sample proband;
    public final Map<String, Sample> items;

    private Samples(Sample proband, Map<String, Sample> items) {
        this.proband = proband;
        this.items = Collections.unmodifiableMap(items);
    }

    public static class Builder {

        private SortedMap<String, Sample> items;

        public Builder(SortedMap<String, Sample> items) {
            this.items = items;
        }

        public Samples build() {
            return new Samples(getProband(items), items);
        }

        private static Sample getProband(Map<String, Sample> items) {
            if (items == null) {
                return null;
            } else if (items.size() == 1) {
                return items.values().iterator().next();
            }
            for (Map.Entry<String, Sample> entry : items.entrySet()) {
                if (isProband(entry.getValue().id)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        private static boolean isProband(String sample) {
            return sample.endsWith("a1");
        }
    }
}
