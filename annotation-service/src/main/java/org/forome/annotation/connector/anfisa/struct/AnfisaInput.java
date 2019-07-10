package org.forome.annotation.connector.anfisa.struct;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Sample;

import java.util.Map;

public class AnfisaInput {

    public final Chromosome chromosome;
    public final long start;
    public final long end;
    public final JSONObject vepJson;
    public final VariantContext variantContext;
    public final Map<String, Sample> samples;

    private AnfisaInput(
            Chromosome chromosome,
            long start, long end,
            JSONObject vepJson,
            VariantContext variantContext,
            Map<String, Sample> samples
    ) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.vepJson = vepJson;
        this.variantContext = variantContext;
        this.samples = samples;


        //Валидируем хромосому
        //TODO Ulitin V. Необходима валидация хромосомы!!!

//        vepJson.getAsString("seq_region_name")
    }

    public static class Builder {

        public final Chromosome chromosome;
        public final long start;
        public final long end;
        private JSONObject vepJson;
        private VariantContext variantContext;
        private Map<String, Sample> samples;

        public Builder(
                Chromosome chromosome,
                long start, long end
        ) {
            this.chromosome = chromosome;
            this.start = start;
            this.end = end;
        }

        public Builder withVepJson(JSONObject vepJson) {
            this.vepJson = vepJson;
            return this;
        }

        public Builder withVariantContext(VariantContext variantContext) {
            this.variantContext = variantContext;
            return this;
        }

        public Builder withSamples(Map<String, Sample> samples) {
            this.samples = samples;
            return this;
        }

        public AnfisaInput build() {
            return new AnfisaInput(
                    chromosome,
                    start, end,
                    vepJson,
                    variantContext,
                    samples
            );
        }
    }
}
