package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.struct.Sample;

import java.util.Map;

public class AnfisaInput {

    public final Map<String, Sample> samples;

    private AnfisaInput(

            Map<String, Sample> samples
    ) {
        this.samples = samples;
    }

    public static class Builder {

        private Map<String, Sample> samples;

        public Builder() {}

        public Builder withSamples(Map<String, Sample> samples) {
            this.samples = samples;
            return this;
        }

        public AnfisaInput build() {
            return new AnfisaInput(
                    samples
            );
        }
    }
}
