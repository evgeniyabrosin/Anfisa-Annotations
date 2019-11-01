package org.forome.annotation.struct.mcase;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class MCase {

    public final Sample proband;
    public final Map<String, Sample> samples;
    public final List<Cohort> cohorts;

    private MCase(Sample proband, Map<String, Sample> samples, List<Cohort> cohorts) {
        this.proband = proband;
        this.samples = Collections.unmodifiableMap(samples);
        this.cohorts = Collections.unmodifiableList(cohorts);
    }

    public static class Builder {

        private SortedMap<String, Sample> samples;
        private List<Cohort> cohorts;

        public Builder(SortedMap<String, Sample> samples, List<Cohort> cohorts) {
            this.samples = samples;
            this.cohorts = cohorts;
        }

        public MCase build() {
            return new MCase(getProband(), samples, cohorts);
        }

        private Sample getProband() {
            if (samples == null || samples.isEmpty()) {
                return null;
            }
            Sample proband = samples.get(samples.firstKey());

            //Validation
            for (Map.Entry<String, Sample> entry : samples.entrySet()) {
                if (entry.getValue() == proband) continue;
                if (entry.getValue().id.endsWith("a1")) {
                    throw new RuntimeException("Not valid samples, a1 is not first record");
                }
            }

            return proband;
        }

    }
}
