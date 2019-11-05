package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.connector.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class AnfisaResultFilters {

	public String chromosome;
	public double fs;
	public Long severity;
	public List<String> has_variant = new ArrayList<>();
	public Integer minGq;
	public Integer probandGq;
	public long distFromExon;
	public GnomadResult.Popmax gnomadPopmax;
	public GnomadResult.Popmax gnomadRawPopmax;
	public Double gnomadAfFam;
	public Double gnomadAfPb;
	public Double gnomadDbExomesAf;
	public Double gnomadDbGenomesAf;
	public double qd;
	public Double mq;
	public List<String> filters;

	public Boolean clinvarBenign;
	public Boolean hgmdBenign;

	public Optional<Boolean> clinvarTrustedBenign;
	public HashMap<String, Integer> altZygosity;

	public String spliceAltering;
	public Float spliceAiDsmax;

	public String alt;

	public Integer numClinvarSubmitters;

	public ClinvarVariantSummary.ReviewStatus clinvarReviewStatus;
	public String[] clinvarAcmgGuidelines;

	public Long gnomadHom;
	public Long gnomadHem;

	public Float cnvLO;

	/**
	 * Список таких когорт, для которых, этот вариант есть хотя бы у одного сэмпла из когорты.
	 */
	public String[] cohortHasVariant;

	public String topTissue;
	public String[] topTissues;

	public String[] pharmacogenomicsDiseases;
	public String[] pharmacogenomicsChemicals;

	public AnfisaResultFilters() {
	}


	public JSONObject toJSON(AnfisaResultData anfisaResultData,
							 AnfisaResultView.Bioinformatics bioinformatics
	) {
		JSONObject out = new JSONObject();
		if (chromosome != null) {
			out.put("chromosome", chromosome);
		}
		if (gnomadPopmax != null) {
			out.put("gnomad_popmax", gnomadPopmax.group.name());
			out.put("gnomad_popmax_af", gnomadPopmax.af);
			out.put("gnomad_popmax_an", gnomadPopmax.an);
		}
		if (gnomadRawPopmax != null) {
			out.put("gnomad_raw_popmax", gnomadRawPopmax.group.name());
			out.put("gnomad_raw_popmax_af", gnomadRawPopmax.af);
			out.put("gnomad_raw_popmax_an", gnomadRawPopmax.an);
		}
		out.put("gnomad_af_fam", gnomadAfFam);
		out.put("severity", severity);
		out.put("gnomad_af_pb", gnomadAfPb);
		out.put("gnomad_db_exomes_af", gnomadDbExomesAf);
		out.put("gnomad_db_genomes_af", gnomadDbGenomesAf);
		out.put("has_variant", new JSONArray());
		out.put("min_gq", minGq);
		out.put("dist_from_exon", distFromExon);
		out.put("proband_gq", probandGq);
		out.put("fs", fs);
		out.put("qd", qd);

		if (clinvarBenign != null) {
			out.put("clinvar_benign", clinvarBenign);
		}
		if (clinvarTrustedBenign != null) {
			out.put("clinvar_trusted_benign", clinvarTrustedBenign.orElse(null));
		}
		if (hgmdBenign != null) {
			out.put("hgmd_benign", hgmdBenign);
		}
		if (mq != null) {
			out.put("mq", mq);
		}
		if (filters != null) {
			out.put("filters", filters);
		}
		if (has_variant != null) {
			out.put("has_variant", has_variant);
		}

		if (altZygosity != null) {
			out.put("alt_zygosity", altZygosity);
		}

		out.put("splice_altering", spliceAltering);
		out.put("splice_ai_dsmax", spliceAiDsmax);

		out.put("gerp_rs", (bioinformatics.conservation != null) ? bioinformatics.conservation.gerpRS : null);

		out.put("ref", anfisaResultData.ref);
		out.put("alt", alt);

		out.put("num_clinvar_submitters", numClinvarSubmitters);

		if (clinvarReviewStatus != null) {
			out.put("clinvar_review_status", clinvarReviewStatus.text);
			out.put("clinvar_criteria_provided", (clinvarReviewStatus.getCriteriaProvided() != null) ?
					clinvarReviewStatus.getCriteriaProvided() : "Unknown");
			out.put("clinvar_conflicts", (clinvarReviewStatus.getConflicts() != null) ?
					clinvarReviewStatus.getConflicts() : "Unknown");
			out.put("clinvar_stars", clinvarReviewStatus.getStars());
			out.put("clinvar_acmg_guidelines", clinvarAcmgGuidelines);
		}

		out.put("gnomad_hom", gnomadHom);
		out.put("gnomad_hem", gnomadHem);

		out.put("cnv_lo", cnvLO);

		out.put("cohort_has_variant", cohortHasVariant);

		out.put("top_tissue", topTissue);
		out.put("top_tissues", topTissues);


		out.put("pharmacogenomics_diseases", (pharmacogenomicsDiseases != null && pharmacogenomicsDiseases.length != 0) ? pharmacogenomicsDiseases : null);
		out.put("pharmacogenomics_chemicals", (pharmacogenomicsChemicals != null && pharmacogenomicsChemicals.length != 0) ? pharmacogenomicsChemicals : null);

		return out;
	}
}
