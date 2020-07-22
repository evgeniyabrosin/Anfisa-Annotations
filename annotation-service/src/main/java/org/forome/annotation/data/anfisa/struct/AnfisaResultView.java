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
import org.forome.annotation.data.conservation.struct.Conservation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class AnfisaResultView {

	public class Databases {
		public String[] clinVar;

		public String[] clinVarVariants;
		public String[] clinVarPhenotypes;
		public String hgmd;
		public String hgmdHg38;
		public String[] hgmdTags;
		public String[] hgmdPhenotypes;
		public final LinkedHashSet<String> references = new LinkedHashSet<>();
		public String[] clinVarSubmitters;
		public String lmmSignificance;
		public String geneDxSignificance;
		public String[] clinVarSignificance;
		public Integer numClinvarSubmitters;
		public String[] clinvarAcmgGuidelines;
		public String clinvarReviewStatus;

		public void setField(String name, String value) {
			switch (name) {
				case "lmm_significance":
					lmmSignificance = value;
					break;
				case "gene_dx_significance":
					geneDxSignificance = value;
					break;
				default:
					throw new RuntimeException("not support: " + name);
			}
		}

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			if (clinVar != null) {
				out.put("clinVar", clinVar);
			}
			out.put("references", (!references.isEmpty()) ? references : null);
			out.put("clinVar_variants", clinVarVariants);
			out.put("clinVar_phenotypes", clinVarPhenotypes);
			out.put("hgmd", hgmd);
			if (hgmdHg38 != null) {
				out.put("hgmd_hg38", hgmdHg38);
			}
			out.put("hgmd_tags", hgmdTags);
			out.put("gene_dx_significance", geneDxSignificance);
			out.put("hgmd_phenotypes", hgmdPhenotypes);
			out.put("clinVar_submitters", clinVarSubmitters);
			out.put("lmm_significance", lmmSignificance);
			out.put("clinVar_significance", clinVarSignificance);
			out.put("num_clinvar_submitters", numClinvarSubmitters);
			out.put("clinvar_acmg_guidelines", clinvarAcmgGuidelines);
			out.put("clinvar_review_status", clinvarReviewStatus);
			return out;
		}
	}

	public class Predictions {
		public List<String> polyphen2Hvar;
		public List<String> polyphen2Hdiv;
		public List<Double> polyphen2HvarScore;
		public List<Double> polyphen2HdivScore;
		public List<String> maxEntScan;
		public String[] sift;
		public String[] siftVEP;
		public String[] polyphen;
		public List<Double> revel;
		public Double[] siftScore;
		public String[] mutationTaster;
		public String[] fathmm;
		public List<Double> lofScore = new ArrayList<>();
		public List<Double> lofScoreCanonical = new ArrayList<>();
		public List<Double> caddPhred;
		public List<Double> caddRaw;
		public String[] mutationAssessor;

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			out.put("polyphen2_hdiv_score", polyphen2HdivScore);
			out.put("mutation_assessor", mutationAssessor);
			out.put("fathmm", fathmm);
			out.put("polyphen2_hdiv", polyphen2Hdiv);
			out.put("polyphen2_hvar", polyphen2Hvar);
			out.put("max_ent_scan", maxEntScan);
			out.put("sift", sift);
			out.put("sift_vep", siftVEP);
			out.put("polyphen", polyphen);
			out.put("revel", revel);
			out.put("polyphen2_hvar_score", polyphen2HvarScore);
			out.put("sift_score", siftScore);
			out.put("cadd_phred", caddPhred);
			out.put("lof_score_canonical", lofScoreCanonical);
			out.put("cadd_raw", caddRaw);
			out.put("mutation_taster", mutationTaster);
			out.put("lof_score", lofScore);
			return out;
		}
	}

	public static class GnomAD {
		public String allele;
		public Long exomeAn;
		public Double exomeAf;
		public Long genomeAn;
		public Double genomeAf;
		public Double af;
		public String[] url;
		public String proband;
		public List<Double> pli;
		public String popmax;
		public String rawPopmax;
		public Long hom;
		public Long hem;

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			if (allele != null) {
				out.put("exome_an", exomeAn);
				out.put("af", af);
				out.put("url", url);
				out.put("exome_af", exomeAf);
				out.put("proband", proband);
				out.put("genome_an", genomeAn);
				out.put("genome_af", genomeAf);
				out.put("allele", allele);
				out.put("pli", pli);
				out.put("popmax", popmax);
				out.put("raw_popmax", rawPopmax);
				out.put("hom", hom);
				out.put("hem", hem);
			} else {
				out.put("url", url);
			}
			return out;
		}
	}

    public class General {
        public String probandGenotype;
        public String maternalGenotype;
        public String paternalGenotype;
        public List<String> canonicalAnnotation = new ArrayList<>();
        public String alt;
        public String ref;
        public List<String> cposWorst = new ArrayList<>();
        public List<String> cposCanonical = new ArrayList<>();
        public List<String> cposOther = new ArrayList<>();
        public List<String> spliceRegion;
        public String[] genes;
        public String worstAnnotation;
        public List<String> geneSplicer;
        public List<String> pposWorst = new ArrayList<>();
        public List<String> pposCanonical = new ArrayList<>();
        public List<String> pposOther = new ArrayList<>();
        public List<String> variantExonWorst = new ArrayList<>();
        public List<String> variantIntronWorst = new ArrayList<>();
        public List<String> variantExonCanonical = new ArrayList<>();
        public List<String> variantIntronCanonical = new ArrayList<>();
        public Optional<String> spliceAltering;

		/**
		 * Содержится набор органов на которые с большой вероятностью вляет затронутый ген
		 */
		public List<String> mostlyExpressed = new ArrayList<>();

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			out.put("proband_genotype", probandGenotype);
			out.put("canonical_annotation", canonicalAnnotation);
			if (alt != null) {
				out.put("alt", alt);
			}
			if (ref != null) {
				out.put("ref", ref);
			}
			out.put("ppos_canonical", pposCanonical);
			out.put("cpos_worst", cposWorst);
			out.put("variant_intron_canonical", variantIntronCanonical);
			out.put("cpos_other", cposOther);
			out.put("splice_region", spliceRegion);
			out.put("maternal_genotype", maternalGenotype);
			out.put("variant_intron_worst", variantIntronWorst);
			out.put("paternal_genotype", paternalGenotype);
			out.put("genes", genes);
			out.put("variant_exon_worst", variantExonWorst);
			out.put("worst_annotation", worstAnnotation);
			out.put("cpos_canonical", cposCanonical);
			out.put("variant_exon_canonical", variantExonCanonical);
			out.put("gene_splicer", geneSplicer);
			out.put("ppos_worst", pposWorst);
			out.put("ppos_other", pposOther);
			if (spliceAltering != null) {
				out.put("splice_altering", spliceAltering.orElse(null));
			}
			out.put("mostly_expressed", mostlyExpressed);
			return out;
		}
	}

	public static class Bioinformatics {

		public String regionCanonical;
		public String regionWorst;

		public String inheritedFrom;
		public long[] distFromExonWorst = new long[0];
		public long[] distFromExonCanonical = new long[0];
		public Conservation conservation;
		public String speciesWithVariant;
		public String speciesWithOthers;
		public List<String> maxEntScan;
		public String nnSplice;
		public String humanSplicingFinder;
		public String[] otherGenes;
		public String[] calledBy;
		public Map<String, Serializable> callerData;
		public Map<String, Float> spliceAi;
		public List<String> spliceAiAg = new ArrayList<>();
		public List<String> spliceAiAl = new ArrayList<>();
		public List<String> spliceAiDg = new ArrayList<>();
		public List<String> spliceAiDl = new ArrayList<>();
		public Float cnvLO;

		public List<String> getSpliceAiValues(String key) {
			switch (key) {
				case "AG":
					return spliceAiAg;
				case "AL":
					return spliceAiAl;
				case "DG":
					return spliceAiDg;
				case "DL":
					return spliceAiDl;
				default:
					throw new RuntimeException("Unknown key: " + key);
			}
		}

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			out.put("region_canonical", regionCanonical);
			out.put("region_worst", regionWorst);
			out.put("human_splicing_finder", humanSplicingFinder);
			out.put("dist_from_exon_worst", distFromExonWorst);
			out.put("called_by", calledBy);
			out.put("max_ent_scan", maxEntScan);
			out.put("dist_from_exon_canonical", distFromExonCanonical);
			out.put("conservation", build(conservation));
			out.put("caller_data", callerData);
			out.put("nn_splice", nnSplice);
			out.put("species_with_variant", speciesWithVariant);
			out.put("other_genes", otherGenes);
			out.put("species_with_others", speciesWithOthers);
			out.put("inherited_from", inheritedFrom);
			out.put("splice_ai", spliceAi);
			out.put("splice_ai_ag", spliceAiAg);
			out.put("splice_ai_al", spliceAiAl);
			out.put("splice_ai_dg", spliceAiDg);
			out.put("splice_ai_dl", spliceAiDl);
			out.put("gerp_rs", (conservation != null) ? conservation.gerpRS : null);
			out.put("cnv_lo", cnvLO);
			return out;
		}

		private static JSONObject build(Conservation conservation) {
			if (conservation == null) {
				return null;
			}
			JSONObject out = new JSONObject();
			out.put("gerp_r_s", conservation.gerpRS);
			out.put("gerp_n", conservation.gerpN);
			return out;
		}
	}

	public static class Pharmacogenomics {

		public static class Item {

			public final String association;
			public final String value;

			public Item(String association, String value) {
				this.association = association;
				this.value = value;
			}

			protected JSONObject toJSON() {
				JSONObject out = new JSONObject();
				out.put("association", association);
				out.put("value", value);
				return out;
			}
		}

		public List<Item> notes = new ArrayList<>();
		public List<Item> pmids = new ArrayList<>();
		public List<Item> diseases = new ArrayList<>();
		public List<Item> chemicals = new ArrayList<>();

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			out.put("notes", (!notes.isEmpty()) ? notes.stream().map(item -> item.toJSON()).collect(Collectors.toList()) : null);
			out.put("pmids", (!pmids.isEmpty()) ? pmids.stream().map(item -> item.toJSON()).collect(Collectors.toList()) : null);
			out.put("diseases", (!diseases.isEmpty()) ? diseases.stream().map(item -> item.toJSON()).collect(Collectors.toList()) : null);
			out.put("chemicals", (!chemicals.isEmpty()) ? chemicals.stream().map(item -> item.toJSON()).collect(Collectors.toList()) : null);
			return out;
		}
	}


	public final JSONObject inheritance;
	public final Databases databases;
	public final Predictions predictions;
	public GnomAD gnomAD;
	public final General general;
	public final Bioinformatics bioinformatics;
	public final JSONArray qualitySamples;
	public HashMap<String, HashMap<String, Float>> cohorts = new HashMap<>();
	public final Pharmacogenomics pharmacogenomics = new Pharmacogenomics();

	public AnfisaResultView() {
		this.inheritance = new JSONObject();
		this.databases = new Databases();
		this.predictions = new Predictions();
		this.general = new General();
		this.bioinformatics = new Bioinformatics();
		this.qualitySamples = new JSONArray();
	}

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();
		out.put("inheritance", inheritance);
		out.put("databases", databases.toJSON());
		out.put("predictions", predictions.toJSON());
		out.put("gnomAD", gnomAD.toJSON());
		out.put("general", general.toJSON());
		out.put("bioinformatics", bioinformatics.toJSON());
		out.put("quality_samples", qualitySamples);
		if (cohorts.size() == 1) {//Когорт нет и есть тольок группа ALL
			out.put("cohorts", null);
		} else {
			out.put("cohorts", cohorts);
		}
		if (!qualitySamples.isEmpty()) {
			out.put("quality_samples", qualitySamples);
		}

		out.put("pharmacogenomics", pharmacogenomics.toJSON());

		return out;
	}
}