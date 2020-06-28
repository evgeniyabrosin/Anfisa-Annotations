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

package org.forome.annotation.processing.graphql.record.view.facets;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacet;


@GraphQLName("record_view_facet")
public class GRecordViewFacet {

	private final DbNSFPItemFacet facet;

	public GRecordViewFacet(DbNSFPItemFacet facet) {
		this.facet = facet;
	}

	@GraphQLField
	@GraphQLName("revel_score")
	public Double getRevelScore() {
		return facet.revelScore;
	}

	@GraphQLField
	@GraphQLName("sift_converted_rank_score")
	public Double getSiftConvertedRankScore() {
		return facet.siftConvertedRankScore;
	}

	@GraphQLField
	@GraphQLName("sift4g_converted_rank_score")
	public Double getSift4gConvertedRankScore() {
		return facet.sift4gConvertedRankScore;
	}

	@GraphQLField
	@GraphQLName("meta_lr_score")
	public Double getMetaLRScore() {
		return facet.metaLRScore;
	}

	@GraphQLField
	@GraphQLName("meta_lr_rank_score")
	public Double getMetaLRRankScore() {
		return facet.metaLRRankScore;
	}

	@GraphQLField
	@GraphQLName("meta_lr_prediction")
	public String getMetaLRPrediction() {
		return facet.metaLRPrediction;
	}

	@GraphQLField
	@GraphQLName("mut_pred_score")
	public Double getMutPredScore() {
		return facet.mutPredScore;
	}

	@GraphQLField
	@GraphQLName("mut_pred_rank_score")
	public Double getMutPredRankScore() {
		return facet.mutPredRankScore;
	}

	@GraphQLField
	@GraphQLName("mut_pred_prot_id")
	public String getMutPredProtID() {
		return facet.mutPredProtID;
	}

	@GraphQLField
	@GraphQLName("mut_pred_aa_change")
	public String getMutPredAAChange() {
		return facet.mutPredAAChange;
	}

	@GraphQLField
	@GraphQLName("mut_pred_top5_features")
	public String[] getMutPredTop5Features() {
		return facet.mutPredTop5Features;
	}

	@GraphQLField
	@GraphQLName("mpc_rank_score")
	public Double getMpcRankScore() {
		return facet.mpcRankScore;
	}

	@GraphQLField
	@GraphQLName("primate_ai_score")
	public Double getPrimateAiScore() {
		return facet.primateAiScore;
	}

	@GraphQLField
	@GraphQLName("primate_ai_rank_score")
	public Double getPrimateAiRankScore() {
		return facet.primateAiRankScore;
	}

	@GraphQLField
	@GraphQLName("refcodon")
	public String getRefcodon() {
		return facet.refcodon;
	}

	@GraphQLField
	@GraphQLName("codonpos")
	public String getCodonpos() {
		return facet.codonpos;
	}


}
