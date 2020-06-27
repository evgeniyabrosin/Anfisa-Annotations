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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItem;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacetTranscript;
import org.forome.annotation.processing.graphql.record.view.transcripts.item.GRecordViewTranscriptsItem;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("record_view_transcripts")
public class GRecordViewTranscripts {

	public final Variant variant;
	public final GContext gContext;

	public GRecordViewTranscripts(Variant variant, GContext gContext) {
		this.variant = variant;
		this.gContext = gContext;
	}

	@GraphQLField
	@GraphQLName("items")
	public List<GRecordViewTranscriptsItem> getItems() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.context, variant
		);

		if (variant instanceof VariantVep) {
			VariantVep variantVep = (VariantVep) variant;
			JSONArray jTranscripts = (JSONArray) variantVep.getVepJson().get("transcript_consequences");
			if (jTranscripts == null) {
				return null;
			}

			List<GRecordViewTranscriptsItem> transcripts = new ArrayList<>();
			for (Object ojTranscript : jTranscripts) {
				JSONObject jTranscript = (JSONObject) ojTranscript;

				String transcriptId = jTranscript.getAsString("transcript_id");

				List<DbNSFPItemFacetTranscript> findTranscripts = items.stream()
						.flatMap(dbNSFPItem -> dbNSFPItem.facets.stream())
						.flatMap(dbNSFPItemFacet -> dbNSFPItemFacet.transcripts.stream())
						.filter(dbNSFPItemFacetTranscript -> transcriptId.equals(dbNSFPItemFacetTranscript.ensemblTranscriptId))
						.collect(Collectors.toList());
				if (findTranscripts.size() > 1) {
					throw new RuntimeException("Not unique transcriptId:" + transcriptId + ", values: " + gContext.context.sourceAStorageHttp.toJSONString());
				}
				DbNSFPItemFacetTranscript dbNSFPTranscript = (findTranscripts.isEmpty()) ? null : findTranscripts.get(0);

				transcripts.add(new GRecordViewTranscriptsItem(
						transcriptId, variantVep, jTranscript, dbNSFPTranscript
				));
			}
			return transcripts;
		} else {
			return null;
		}
	}
}
