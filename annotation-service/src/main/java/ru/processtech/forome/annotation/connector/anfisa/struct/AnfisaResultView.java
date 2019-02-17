package ru.processtech.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AnfisaResultView {

	public class Databases {
		public String[] clinVar;

		public Object beacons;
		public String[] pubmedSearch;
		public String[] clinVarVariants;
		public String[] clinVarPhenotypes;
		public String hgmd;
		public String hgmdHg38;
		public String[] hgmdTags;
		public String[] hgmdPhenotypes;
		public String[] hgmdPmids;
		public String[] omim;
		public String[] beaconUrl;
		public String[] clinVarSubmitters;
		public String lmmSignificance;
		public String geneDxSignificance;
		public String[] geneCards;
		public String[] clinVarSignificance;

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
	}

	public class Predictions {
		public JSONArray polyphen2HdivScore = new JSONArray();
		public JSONArray mutationAssessor = new JSONArray();
		public JSONArray fathmm = new JSONArray();
		public JSONArray polyphen2Hdiv = new JSONArray();
		public JSONArray polyphen2Hvar = new JSONArray();
		public Object maxEntScan;
		public String[] sift;
		public String[] polyphen;
		public JSONArray revel = new JSONArray();
		public JSONArray polyphen2HvarScore = new JSONArray();
		public Double[] siftScore;
		public JSONArray caddPhred = new JSONArray();
		public JSONArray lofScoreCanonical = new JSONArray();
		public JSONArray caddRaw = new JSONArray();
		public JSONArray mutationTaster = new JSONArray();
		public JSONArray lofScore = new JSONArray();
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
		public Object pli;
		public String popMax;
	}

	public class General {
		public Object probandGenotype;
		public Object canonicalAnnotation;
		public JSONArray ensemblTranscriptsCanonical = new JSONArray();
		public String alt;
		public JSONArray pposCanonical = new JSONArray();
		public JSONArray cposWorst = new JSONArray();
		public JSONArray variantIntronCanonical = new JSONArray();
		public JSONArray cposOther = new JSONArray();
		public JSONArray spliceRegion = new JSONArray();
		public Object maternalGenotype;
		public JSONArray variantIntronWorst = new JSONArray();
		public Object paternalGenotype;
		public String ref;
		public String[] genes;
		public JSONArray variantExonWorst = new JSONArray();
		public Object igv;
		public String worstAnnotation;
		public JSONArray cposCanonical = new JSONArray();
		public JSONArray variantExonCanonical = new JSONArray();
		public JSONArray refseqTranscriptWorst = new JSONArray();
		public JSONArray refseqTranscriptCanonical = new JSONArray();
		public JSONArray geneSplicer = new JSONArray();
		public JSONArray pposWorst = new JSONArray();
		public JSONArray pposOther = new JSONArray();
		public String hg38;
		public String hg19;
		public JSONArray ensemblTranscriptsWorst = new JSONArray();
	}

	public class Bioinformatics {
		public String humanSplicingFinder;
		public Object zygosity;
		public JSONArray distFromExonWorst = new JSONArray();
		public Object calledBy;
		public Object maxEntScan;
		public JSONArray distFromExonCanonical = new JSONArray();
		public JSONArray conservation = new JSONArray();
		public JSONObject callerData = new JSONObject();
		public String nnSplice;
		public String speciesWithVariant;
		public String[] otherGenes;
		public String speciesWithOthers;
		public Object inheritedFrom;
	}

	public final JSONObject inheritance;
	public final Databases databases;
	public final Predictions predictions;
	public final List<GnomAD> gnomAD;
	public final General general;
	public final Bioinformatics bioinformatics;
	public final JSONArray qualitSamples;

	public AnfisaResultView() {
		this.inheritance = new JSONObject();
		this.databases = new Databases();
		this.predictions = new Predictions();
		this.gnomAD = new ArrayList<>();
		this.general = new General();
		this.bioinformatics = new Bioinformatics();
		this.qualitSamples = new JSONArray();
	}
}


/**
 * {
 * "bioinformatics": {
 * "human_splicing_finder": "",
 * "zygosity": null,
 * "dist_from_exon_worst": [
 * <p>
 * ],
 * "called_by": null,
 * "max_ent_scan": null,
 * "dist_from_exon_canonical": [
 * <p>
 * ],
 * "conservation": [
 * <p>
 * ],
 * "caller_data": {
 * <p>
 * },
 * "nn_splice": "",
 * "species_with_variant": "",
 * "other_genes": [
 * <p>
 * ],
 * "species_with_others": "",
 * "inherited_from": null
 * },
 * "quality_samples": [
 * <p>
 * ]
 * }
 * }
 */