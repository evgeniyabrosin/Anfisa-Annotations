package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;

import java.util.List;
import java.util.Map;

public class AnfisaResultData {

	public String totalExonIntronCanonical;
	public String assemblyName;
	public Long end;
	public Long start;
	public String alt;
	public String ref;
	public JSONArray regulatoryFeatureConsequences;
	public JSONArray motifFeatureConsequences;
	public JSONArray intergenicConsequences;
	public String[] hgmdPmids;
	public String variantExonIntronWorst;
	public String variantExonIntronCanonical;
	public String mostSevereConsequence;
	public String alleleString;
	public String seqRegionName;
	public String totalExonIntronWorst;
	public String[] beaconUrls;
	public String input;
	public String label;
	public JSONArray transcriptConsequences;
	public String id;
	public Long strand;
	public ColorCode.Code colorCode;

	public String lmm;
	public String geneDx;

	public JSONArray colocatedVariants;

	public String hgmd;
	public String hgmdHg38;
	public Long[] clinVar;
	public String[] clinvarVariants;
	public String[] clinvarPhenotypes;
	public String[] clinvarSignificance;

	public Map<String, String> clinvarSubmitters;

	public String variantClass;

    public List<Object[]> distFromBoundaryCanonical;
    public String regionCanonical;
    public List<Object[]> distFromBoundaryWorst;
    public String regionWorst;

    public Map<String, Integer> zygosity;

    public Map<String, SpliceAIResult.DictSql> spliceAI;

	public Map<String, String> cnvGT;

	public String version;

	public void setField(String name, String value) {
		switch (name) {
			case "lmm":
				lmm = value;
				break;
			case "gene_dx":
				geneDx = value;
				break;
			default:
				throw new RuntimeException("not support: " + name);
		}
	}

	public String getField(String name) {
		switch (name) {
			case "lmm":
				return lmm;
			case "gene_dx":
				return geneDx;
			default:
				throw new RuntimeException("not support: " + name);
		}
	}
}
