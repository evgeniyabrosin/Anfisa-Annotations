/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.processing.graphql.record.view.transcripts;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.struct.GtfAnfisaResult;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacetTranscript;
import org.forome.annotation.processing.graphql.record.view.general.transcript.GRecordViewGeneralTranscript;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GraphQLName("record_view_transcript")
public class GRecordViewTranscript extends GRecordViewGeneralTranscript {

	private final GContext gContext;
	private final DbNSFPItemFacetTranscript dbNSFPTranscript;
	private final GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distanceFromBoundary;

	public GRecordViewTranscript(
			GContext gContext,
			String transcriptId,
			VariantVep variantVep, JSONObject jTranscript,
			DbNSFPItemFacetTranscript dbNSFPTranscript,
			GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distanceFromBoundary
	) {
		super(transcriptId, variantVep, jTranscript);

		this.gContext = gContext;

		this.dbNSFPTranscript = dbNSFPTranscript;
		this.distanceFromBoundary = distanceFromBoundary;
	}

	@GraphQLField
	@GraphQLName("biotype")
	public String getBioType() {
		return biotype;
	}

	@GraphQLField
	@GraphQLName("codons")
	public String getCodons() {
		return jTranscript.getAsString("codons");
	}

	@GraphQLField
	@GraphQLName("amino_acids")
	public String getAminoAcids() {
		return jTranscript.getAsString("amino_acids");
	}


	@GraphQLField
	@GraphQLName("transcript_source")
	public String getSource() {
		return source;
	}

	@GraphQLField
	@GraphQLName("cpos")
	public String getCPos() {
		String hgvsc = jTranscript.getAsString("hgvsc");
		if (hgvsc == null) {
			return null;
		}

		//В основном мы имеем формат: ENST00000323084.9:c.83-5969A>G
		//Но в кейсе pgp3140_wgs_nist-v3.3.2 вариант: chr1:249168516 A>G
		//встретили: NC_000001.10:TRNAE24:u_t_1.1:n.70A>G

		String[] splitHgvsc = hgvsc.split(":");
		if (splitHgvsc.length < 2) {
			throw new RuntimeException("Unsupported situation, " + variantVep
					+ ", transcript.id: " + getId() + ", hgvsc: " + hgvsc);
		}

		return splitHgvsc[splitHgvsc.length - 1];
	}

	@GraphQLField
	@GraphQLName("ppos")
	public String getPPos() {
		String hgvsp = jTranscript.getAsString("hgvsp");
		if (hgvsp == null) {
			return null;
		}

		//В основном мы имеем формат: ENSP00000383225.3:p.Cys72=
		//Но в кейсе pgp3140_wgs_nist-v3.3.2 вариант: chr7:38335520 T>G
		//встретили: NC_000007.13:TRGVB:u_t_1.1:p.Lys68Asn

		String[] splitHgvsp = hgvsp.split(":");
		if (splitHgvsp.length < 2) {
			throw new RuntimeException("Unsupported state, " + variantVep
					+ ", transcript.id: " + getId() + ", hgvsp: " + hgvsp);
		}

		String value = splitHgvsp[splitHgvsp.length - 1];
		if (!value.startsWith("p.")) {
			throw new RuntimeException("Unsupported state, " + variantVep
					+ ", transcript.id: " + getId() + ", hgvsp: " + hgvsp);
		}

		return "p." + convertPPos(value.substring(2));
	}

	@GraphQLField
	@GraphQLName("variant_exon")
	public String getVariantExon() {
		return jTranscript.getAsString("exon");
	}

	@GraphQLField
	@GraphQLName("variant_intron")
	public String getVariantIntron() {
		return jTranscript.getAsString("intron");
	}

	@GraphQLField
	@GraphQLName("ensembl_gene_id")
	public String getEnsemblGeneId() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.ensemblGeneId;
	}

	@GraphQLField
	@GraphQLName("ensembl_protein_id")
	public String getEnsemblProteinId() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.ensemblProteinId;
	}

	@GraphQLField
	@GraphQLName("uniprot_acc")
	public String getUniprotAcc() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.uniprotAcc;
	}

	@GraphQLField
	@GraphQLName("hgvs_c_annovar")
	public String getHgvsCAnnovar() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.hgvsCAnnovar;
	}

	@GraphQLField
	@GraphQLName("hgvs_p_annovar")
	public String getHgvsPAnnovar() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.hgvsPAnnovar;
	}

	@GraphQLField
	@GraphQLName("hgvs_c_snp_eff")
	public String getHgvsCSnpEff() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.hgvsCSnpEff;
	}

	@GraphQLField
	@GraphQLName("hgvs_p_snp_eff")
	public String getHgvsPSnpEff() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.hgvsPSnpEff;
	}

	@GraphQLField
	@GraphQLName("gencode_basic")
	public Boolean getGencodeBasic() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.gencodeBasic;
	}

	@GraphQLField
	@GraphQLName("sift_score")
	public Double getSiftScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.siftScore;
	}

	@GraphQLField
	@GraphQLName("sift_prediction")
	public String getSiftPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.siftPrediction;
	}

	@GraphQLField
	@GraphQLName("sift_4g_score")
	public Double getSift4GScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.sift4GScore;
	}

	@GraphQLField
	@GraphQLName("sift_4g_prediction")
	public String getSift4GPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.sift4GPrediction;
	}

	@GraphQLField
	@GraphQLName("polyphen2_hdiv_score")
	public Double getPolyphen2HdivScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.polyphen2HDIVScore;
	}

	@GraphQLField
	@GraphQLName("polyphen2_hdiv_prediction")
	public String getPolyphen2HdivPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.polyphen2HDIVPred;
	}

	@GraphQLField
	@GraphQLName("polyphen2_hvar_score")
	public Double getPolyphen2HvarScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.polyphen2HVARScore;
	}

	@GraphQLField
	@GraphQLName("polyphen2_hvar_prediction")
	public String getPolyphen2HvarPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.polyphen2HVARPred;
	}

	@GraphQLField
	@GraphQLName("mutation_assessor_prediction")
	public String getmutationAssessorPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.mutationAssessorPred;
	}

	@GraphQLField
	@GraphQLName("mutation_assessor_score")
	public Double getMutationAssessorScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.mutationAssessorScore;
	}

	@GraphQLField
	@GraphQLName("fathmm_prediction")
	public String getFathmmPrediction() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.fathmmPrediction;
	}

	@GraphQLField
	@GraphQLName("fathmm_score")
	public Double getFathmmScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.fathmmScore;
	}

	@GraphQLField
	@GraphQLName("mpc_score")
	public Double getMpcScore() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.mpcScore;
	}

	@GraphQLField
	@GraphQLName("refcodon")
	public String getRefcodon() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.refcodon;
	}

	@GraphQLField
	@GraphQLName("codonpos")
	public String getCodonpos() {
		return (dbNSFPTranscript == null) ? null : dbNSFPTranscript.codonpos;
	}

	@GraphQLField
	@GraphQLName("dist_from_exon")
	public Long getDistFromExon() {
		if (distanceFromBoundary != null) {
			return distanceFromBoundary.dist;
		} else {
			return null;
		}
	}

	@GraphQLField
	@GraphQLName("region")
	public String getRegion() {
		if (distanceFromBoundary != null) {
			return distanceFromBoundary.region;
		} else {
			return null;
		}
	}

	@GraphQLField
	@GraphQLName("masked_region")
	public boolean getMaskedRegion() {
		return gContext.context.getMaskedRegion(gContext.anfisaConnector);
	}


	private static final Map<String, String> proteins_3_to_1 = new HashMap<String, String>() {{
		put("Ala", "A");
		put("Arg", "R");
		put("Asn", "N");
		put("Asp", "D");
		put("Cys", "C");
		put("Gln", "Q");
		put("Glu", "E");
		put("Gly", "G");
		put("His", "H");
		put("Ile", "I");
		put("Leu", "L");
		put("Lys", "K");
		put("Met", "M");
		put("Phe", "F");
		put("Pro", "P");
		put("Ser", "S");
		put("Thr", "T");
		put("Trp", "W");
		put("Tyr", "Y");
		put("Val", "V");
	}};

	public static String convertPPos(String x) {
		List<Character> protein1 = new ArrayList<>();
		List<Character> pos = new ArrayList<>();
		List<Character> protein2 = new ArrayList<>();
		int state = 0;
		for (char c : x.toCharArray()) {
			if (state == 0) {
				if (Character.isLetter(c)) {
					protein1.add(c);
					continue;
				}
				state = 2;
			}
			if (state == 2) {
				if (Character.isDigit(c)) {
					pos.add(c);
					continue;
				}
				state = 3;
			}
			if (state == 3) {
				protein2.add(c);
			} else {
				break;
			}
		}
		String p1 = protein1.stream().map(c -> c.toString()).collect(Collectors.joining());
		String p2 = protein2.stream().map(c -> c.toString()).collect(Collectors.joining());
		String rpos = pos.stream().map(c -> c.toString()).collect(Collectors.joining());
		String rprotein1 = proteins_3_to_1.getOrDefault(p1, p1);
		String rprotein2 = proteins_3_to_1.getOrDefault(p2, p2);
		return String.format("%s%s%s", rprotein1, rpos, rprotein2);
	}
}
