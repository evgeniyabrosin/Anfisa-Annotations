/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.processing.graphql.view.general;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.processing.graphql.view.general.transcript.GRecordViewGeneralTranscript;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("record_view_general")
public class GRecordViewGeneral {

	public final Variant variant;

	public GRecordViewGeneral(Variant variant) {
		this.variant = variant;
	}

	@GraphQLField
	@GraphQLName("transcripts")
	public List<GRecordViewGeneralTranscript> getTranscripts() {
		if (variant instanceof VariantVep) {
			List<GRecordViewGeneralTranscript> transcripts = new ArrayList<>();

			JSONObject vepJson = ((VariantVep) variant).getVepJson();
			for (Object ojTranscript: (JSONArray) vepJson.get("transcript_consequences")) {
				JSONObject jTranscript = (JSONObject) ojTranscript;
				transcripts.add(new GRecordViewGeneralTranscript(
						jTranscript.getAsString("transcript_id"),
						jTranscript.getAsString("gene_symbol"),
						null
				));
			}
			return transcripts;
		} else {
			return null;
		}
	}

}
