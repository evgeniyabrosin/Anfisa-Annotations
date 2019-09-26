package org.forome.annotation.struct.variant.vep;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;

public class VariantVep extends Variant {

    private JSONObject vepJson;

    public VariantVep(Chromosome chromosome, int start, int end) {
        super(chromosome, start, end);
    }

    public JSONObject getVepJson() {
        return vepJson;
    }

    public void setVepJson(JSONObject vepJson) {
        this.vepJson = vepJson;
    }

    @Override
    public VariantType getVariantType() {
        String value = vepJson.getAsString("variant_class");
        return VariantType.findByName(value);
    }

}
