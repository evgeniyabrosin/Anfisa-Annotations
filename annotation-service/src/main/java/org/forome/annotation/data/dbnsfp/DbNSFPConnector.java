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

package org.forome.annotation.data.dbnsfp;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItem;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacet;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacetTranscript;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.MathUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DbNSFPConnector {

	public List<DbNSFPItem> getAll(Source source, Variant variant) {
		JSONArray jRecords = source.getDbNSFP(variant.getInterval());
		if (jRecords == null) {
			return Collections.emptyList();
		}

		List<JSONObject> records = jRecords.stream()
				.map(o -> (JSONObject) o)
				.filter(item -> item.getAsString("REF").equals(variant.getRef()) && item.getAsString("ALT").equals(variant.getStrAlt()))
				.collect(Collectors.toList());

		return records.stream().map(jsonObject -> _build(jsonObject)).collect(Collectors.toList());
	}

	private static DbNSFPItem _build(JSONObject jsonObject) {

		List<DbNSFPItemFacet> facets = ((JSONArray) jsonObject.get("facets")).stream()
				.map(o -> _buildFacet((JSONObject) o))
				.collect(Collectors.toList());

		String sGeuvadisEQtlTargetGene = jsonObject.getAsString("Geuvadis_eQTL_target_gene");
		List<String> geuvadisEQtlTargetGene = (sGeuvadisEQtlTargetGene == null) ? null :
				Arrays.stream(sGeuvadisEQtlTargetGene.split(";"))
						.filter(s -> !s.isEmpty())
						.collect(Collectors.toList());

		return new DbNSFPItem(
				MathUtils.toDouble(jsonObject.getAsNumber("CADD_raw")),
				MathUtils.toDouble(jsonObject.getAsNumber("CADD_phred")),
				MathUtils.toDouble(jsonObject.getAsNumber("DANN_score")),
				geuvadisEQtlTargetGene,
				facets
		);
	}

	private static DbNSFPItemFacet _buildFacet(JSONObject jsonObject) {
		List<DbNSFPItemFacetTranscript> transcripts = ((JSONArray) jsonObject.get("transcripts")).stream()
				.map(o -> _buildFacetTranscript((JSONObject) o))
				.collect(Collectors.toList());

		String sMutPredTop5Features = jsonObject.getAsString("MutPred_Top5features");
		String[] mutPredTop5Features = (sMutPredTop5Features == null) ? null :
				Arrays.stream(sMutPredTop5Features.split(";")).map(s -> s.trim())
						.toArray(String[]::new);

		return new DbNSFPItemFacet(
				MathUtils.toDouble(jsonObject.getAsNumber("REVEL_score")),
				MathUtils.toDouble(jsonObject.getAsNumber("SIFT_converted_rankscore")),
				MathUtils.toDouble(jsonObject.getAsNumber("SIFT4G_converted_rankscore")),

				MathUtils.toDouble(jsonObject.getAsNumber("MetaLR_score")),
				MathUtils.toDouble(jsonObject.getAsNumber("MetaLR_rankscore")),
				jsonObject.getAsString("MetaLR_pred"),

				MathUtils.toDouble(jsonObject.getAsString("MutPred_score")),
				MathUtils.toDouble(jsonObject.getAsNumber("MetaLR_rankscore")),
				jsonObject.getAsString("MutPred_protID"),
				jsonObject.getAsString("MutPred_AAchange"),
				mutPredTop5Features,

				MathUtils.toDouble(jsonObject.getAsString("MPC_rankscore")),
				MathUtils.toDouble(jsonObject.getAsString("PrimateAI_score")),
				MathUtils.toDouble(jsonObject.getAsString("PrimateAI_rankscore")),

				MathUtils.toDouble(jsonObject.getAsString("CADD_raw_rankscore")),

				jsonObject.getAsString("MutationTaster_pred"),
				jsonObject.getAsString("PrimateAI_pred"),

				transcripts
		);
	}

	private static DbNSFPItemFacetTranscript _buildFacetTranscript(JSONObject jsonObject) {
		return new DbNSFPItemFacetTranscript(
				jsonObject.getAsString("Ensembl_transcriptid"),

				jsonObject.getAsString("MutationAssessor_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("MutationAssessor_score")),

				jsonObject.getAsString("Polyphen2_HVAR_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("Polyphen2_HVAR_score")),

				jsonObject.getAsString("Polyphen2_HDIV_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("Polyphen2_HDIV_score")),

				jsonObject.getAsString("FATHMM_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("FATHMM_score")),

				jsonObject.getAsString("SIFT_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("SIFT_score")),

				jsonObject.getAsString("Ensembl_geneid"),
				jsonObject.getAsString("Ensembl_proteinid"),

				jsonObject.getAsString("Uniprot_acc"),

				jsonObject.getAsString("HGVSc_ANNOVAR"),
				jsonObject.getAsString("HGVSp_ANNOVAR"),
				jsonObject.getAsString("HGVSc_snpEff"),
				jsonObject.getAsString("HGVSp_snpEff"),

				convertToGencodeBasic(jsonObject.getAsString("GENCODE_basic")),

				jsonObject.getAsString("SIFT4G_pred"),
				MathUtils.toDouble(jsonObject.getAsNumber("SIFT4G_score")),

				MathUtils.toDouble(jsonObject.getAsNumber("MPC_score")),

				jsonObject.getAsString("refcodon"),
				jsonObject.getAsString("codonpos")
		);
	}

	private static Boolean convertToGencodeBasic(String value) {
		if (value == null) return null;
		if ("Y".equals(value)) {
			return true;
		} else if ("N".equals(value)) {
			return false;
		} else {
			throw new RuntimeException("Unsupport value: " + value);
		}
	}
}
