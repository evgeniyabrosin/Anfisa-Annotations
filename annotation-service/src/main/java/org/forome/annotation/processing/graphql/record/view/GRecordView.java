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

package org.forome.annotation.processing.graphql.record.view;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItem;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacetTranscript;
import org.forome.annotation.processing.graphql.record.view.bioinformatics.GRecordViewBioinformatics;
import org.forome.annotation.processing.graphql.record.view.facets.GRecordViewFacet;
import org.forome.annotation.processing.graphql.record.view.general.GRecordViewGeneral;
import org.forome.annotation.processing.graphql.record.view.predictions.GRecordViewPredictions;
import org.forome.annotation.processing.graphql.record.view.transcripts.GRecordViewTranscript;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GraphQLName("record_view")
public class GRecordView {

	private final GContext gContext;
	public final MCase mCase;
	public final Variant variant;

	public GRecordView(GContext gContext, MCase mCase, Variant variant) {
		this.gContext = gContext;
		this.mCase = mCase;
		this.variant = variant;
	}

	@GraphQLField
	@GraphQLName("general")
	public GRecordViewGeneral getGeneral() {
		return new GRecordViewGeneral(variant);
	}

	@GraphQLField
	@GraphQLName("bioinformatics")
	public GRecordViewBioinformatics getBioinformatics() {
		return new GRecordViewBioinformatics(gContext, mCase, variant);
	}

	@GraphQLField
	@GraphQLName("predictions")
	public GRecordViewPredictions getPredictions() {
		return new GRecordViewPredictions(gContext, variant);
	}

	@GraphQLField
	@GraphQLName("facets")
	public List<GRecordViewFacet> getFacets() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.context, variant
		);

		List<GRecordViewFacet> facets = items.stream()
				.flatMap(item -> item.facets.stream())
				.map(dbNSFPItemFacet -> new GRecordViewFacet(dbNSFPItemFacet))
				.collect(Collectors.toList());

		return facets;
	}


	@GraphQLField
	@GraphQLName("transcripts")
	public List<GRecordViewTranscript> getTranscripts() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.context, variant
		);

		if (variant instanceof VariantVep) {
			VariantVep variantVep = (VariantVep) variant;
			JSONArray joTranscripts = (JSONArray) variantVep.getVepJson().get("transcript_consequences");
			if (joTranscripts == null) {
				return null;
			}

			Set<String> uniqueTranscriptIds = new HashSet<>();
			List<JSONObject> jTranscripts = joTranscripts.stream()
					.map(o -> (JSONObject) o)
					.filter(item -> uniqueTranscriptIds.add(item.getAsString("transcript_id")))
					.collect(Collectors.toList());

			List<GRecordViewTranscript> transcripts = new ArrayList<>();
			for (JSONObject jTranscript : jTranscripts) {
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

				transcripts.add(new GRecordViewTranscript(
						transcriptId, variantVep, jTranscript, dbNSFPTranscript
				));
			}
			return transcripts;
		} else {
			return null;
		}
	}
}
