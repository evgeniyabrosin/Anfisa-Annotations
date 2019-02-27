package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;

import java.util.Map;

public class AnfisaResultData {

	public Object totalExonIntronCanonical;
	public String assemblyName;
	public Long end;
	public JSONArray regulatoryFeatureConsequences;
	public JSONArray motifFeatureConsequences;
	public JSONArray intergenicConsequences;
	public String[] hgmdPmids;
	public Long start;
	public Object variantExonIntronWorst;
	public Object variantExonIntronCanonical;
	public String mostSevereConsequence;
	public String alleleString;
	public String seqRegionName;
	public Object totalExonIntronWorst;
	public String[] beaconUrls;
	public String input;
	public String label;
	public JSONArray transcriptConsequences;
	public String id;
	public Long strand;
	public String colorCode;

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
