package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.struct.sample.Samples;

import java.util.TreeMap;

public class AnfisaInput {

    public final Samples samples;

    private AnfisaInput(Samples samples) {
        this.samples = samples;
    }

    public static class Builder {

        private Samples samples;

        public Builder() {
            this.samples = new Samples.Builder(new TreeMap<>()).build();
        }

        public Builder withSamples(Samples samples) {
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
