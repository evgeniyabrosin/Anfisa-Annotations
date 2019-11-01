package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.struct.mcase.MCase;

import java.util.Collections;
import java.util.TreeMap;

public class AnfisaInput {

    public final MCase samples;

    private AnfisaInput(MCase samples) {
        this.samples = samples;
    }

    public static class Builder {

        private MCase samples;

        public Builder() {
            this.samples = new MCase.Builder(new TreeMap<>(), Collections.emptyList()).build();
        }

        public Builder withSamples(MCase samples) {
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
