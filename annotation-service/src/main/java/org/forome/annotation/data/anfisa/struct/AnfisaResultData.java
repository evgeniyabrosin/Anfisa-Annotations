/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.anfisa.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.struct.variant.VariantType;

import java.util.List;
import java.util.Map;

public class AnfisaResultData {

	public String totalExonIntronCanonical;
	public String assemblyName;
	public int start;
	public int end;
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
	public String input;
	public JSONArray transcriptConsequences;
	public List<String> id;
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

	public VariantType variantClass;

	public List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distFromBoundaryCanonical;
	public String regionCanonical;
	public List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distFromBoundaryWorst;
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
		if (colocatedVariants != null) {
			out.put("colocated_variants", colocatedVariants);
		}
		out.put("input", input);
		if (transcriptConsequences != null) {
			out.put("transcript_consequences", transcriptConsequences);
		}
		out.put("most_severe_consequence", mostSevereConsequence);
		out.put("strand", strand);

		if (colorCode != null) {
			out.put("color_code", colorCode.code);
		}

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
		out.put("variant_class", variantClass.toJSON());
		if (distFromBoundaryCanonical != null) {
			out.put("dist_from_boundary_canonical", new JSONArray() {{
				for (GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distance : distFromBoundaryCanonical) {
					add(distance.toJSON());
				}
			}});
		}
		if (regionCanonical != null) {
			out.put("region_canonical", regionCanonical);
		}
		if (distFromBoundaryWorst != null) {
			out.put("dist_from_boundary_worst", new JSONArray() {{
				for (GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distance : distFromBoundaryWorst) {
					add(distance.toJSON());
				}
			}});
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
