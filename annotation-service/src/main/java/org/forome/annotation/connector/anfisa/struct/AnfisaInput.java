package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.struct.variant.Variant;

import java.util.Map;

public class AnfisaInput {

    public final Variant variant;
    public final JSONObject vepJson;
    public final Map<String, Sample> samples;

    private AnfisaInput(
            Variant variant,
            JSONObject vepJson,
            Map<String, Sample> samples
    ) {
        this.variant = variant;
        this.vepJson = vepJson;
        this.samples = samples;
    }

    public static class Builder {

        private Variant variant;
        private JSONObject vepJson;
        private Map<String, Sample> samples;

        public Builder(
                Variant variant
        ) {
            this.variant = variant;
        }

        public Builder withVepJson(JSONObject vepJson) {
            this.vepJson = vepJson;
            return this;
        }

        public Builder withSamples(Map<String, Sample> samples) {
            this.samples = samples;
            return this;
        }

        public AnfisaInput build() {
            return new AnfisaInput(
                    variant,
                    vepJson,
                    samples
            );
        }
    }
}
