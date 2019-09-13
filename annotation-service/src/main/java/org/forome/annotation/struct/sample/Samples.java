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
            return new Samples(getProband(), items);
        }

        private Sample getProband() {
            if (items == null || items.isEmpty()) {
                return null;
            }
            Sample proband = items.get(items.firstKey());

            //Validation
            for (Map.Entry<String, Sample> entry : items.entrySet()) {
                if (entry.getValue() == proband) continue;
                if (entry.getValue().id.endsWith("a1")) {
                    throw new RuntimeException("Not valid samples, a1 is not first record");
                }
            }

            return proband;
        }

    }
}
