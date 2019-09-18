package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;

import java.util.List;
import java.util.Map;

public class AnfisaResultData {

	public String totalExonIntronCanonical;
	public String assemblyName;
	public int start;
	public int end;
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

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();
		out.put("total_exon_intron_canonical", totalExonIntronCanonical);
		out.put("assembly_name", assemblyName);
		out.put("start", start);
		out.put("end", end);
		if (alt != null) {
			out.put("alt", alt);
		}
		if (ref != null) {
			out.put("ref", ref);
		}
		if (regulatoryFeatureConsequences != null) {
			out.put("regulatory_feature_consequences", regulatoryFeatureConsequences);
		}
		if (motifFeatureConsequences != null) {
			out.put("motif_feature_consequences", motifFeatureConsequences);
		}
		if (intergenicConsequences != null) {
			out.put("intergenic_consequences", intergenicConsequences);
		}
		out.put("hgmd_pmids", hgmdPmids);
		out.put("variant_exon_intron_worst", variantExonIntronWorst);
		out.put("variant_exon_intron_canonical", variantExonIntronCanonical);
		out.put("id", id);
		out.put("allele_string", alleleString);
		out.put("seq_region_name", seqRegionName);
		out.put("total_exon_intron_worst", totalExonIntronWorst);
		out.put("beacon_url", beaconUrls);
		if (colocatedVariants != null) {
			out.put("colocated_variants", colocatedVariants);
		}
		out.put("input", input);
		out.put("label", label);
		if (transcriptConsequences != null) {
			out.put("transcript_consequences", transcriptConsequences);
		}
		out.put("most_severe_consequence", mostSevereConsequence);
		out.put("strand", strand);
		out.put("color_code", (colorCode != null) ? colorCode.code : null);

		if (lmm != null) {
			out.put("lmm", lmm);
		}
		if (hgmd != null) {
			out.put("HGMD", hgmd);
		}
		if (hgmdHg38 != null) {
			out.put("HGMD_HG38", hgmdHg38);
		}
		if (clinVar != null) {
			out.put("ClinVar", clinVar);
		}
		if (clinvarVariants != null) {
			out.put("clinvar_variants", clinvarVariants);
		}
		if (clinvarPhenotypes != null) {
			out.put("clinvar_phenotypes", clinvarPhenotypes);
		}
		if (clinvarSignificance != null) {
			out.put("clinvar_significance", clinvarSignificance);
		}
		if (clinvarSubmitters != null) {
			out.put("clinvar_submitters", clinvarSubmitters);
		}
		if (geneDx != null) {
			out.put("gene_dx", geneDx);
		}
		if (variantClass != null) {
			out.put("variant_class", variantClass);
		}
		if (distFromBoundaryCanonical != null) {
			out.put("dist_from_boundary_canonical", distFromBoundaryCanonical);
		}
		if (regionCanonical != null) {
			out.put("region_canonical", regionCanonical);
		}
		if (distFromBoundaryWorst != null) {
			out.put("dist_from_boundary_worst", distFromBoundaryWorst);
		}
		if (regionWorst != null) {
			out.put("region_worst", regionWorst);
		}
		if (zygosity != null) {
			out.put("zygosity", zygosity);
		}
		out.put("spliceAI", new JSONObject() {{
			for (Map.Entry<String, SpliceAIResult.DictSql> entry : spliceAI.entrySet()) {
				put(
						entry.getKey(),
						new JSONObject() {{
							put("DP_AG", entry.getValue().dp_ag);
							put("DP_AL", entry.getValue().dp_al);
							put("DP_DG", entry.getValue().dp_dg);
							put("DP_DL", entry.getValue().dp_dl);
							put("DS_AG", entry.getValue().ds_ag);
							put("DS_AL", entry.getValue().ds_al);
							put("DS_DG", entry.getValue().ds_dg);
							put("DS_DL", entry.getValue().ds_dl);
						}}
				);
			}
		}});
		out.put("cnv_gt", cnvGT);
		out.put("version", version);
		return out;
	}
}
