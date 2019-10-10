package org.forome.annotation.struct.variant.vep;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;

import java.util.ArrayList;
import java.util.List;

public abstract class VariantVep extends Variant {

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

    @Override
    public String getRef() {
        return vepJson.getAsString("allele_string").split("/")[0];
    }

    @Override
    public List<String> getAltAllele() {
        String[] ss = vepJson.getAsString("allele_string").split("/");
        List<String> result = new ArrayList<>();
        for (int i = 1; i < ss.length; i++) {
            result.add(ss[i]);
        }
        return result;
    }

    public String getMostSevereConsequence() {
        return vepJson.getAsString("most_severe_consequence");
    }

    @Override
    public String getId(){
        return vepJson.getAsString("id");
    }

    public JSONArray getTranscriptConsequences(){
        return (JSONArray) vepJson.get("transcript_consequences");
    }
}
