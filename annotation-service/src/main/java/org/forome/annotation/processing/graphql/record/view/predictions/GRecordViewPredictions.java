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

package org.forome.annotation.processing.graphql.record.view.predictions;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItem;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.variant.Variant;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@GraphQLName("record_view_predictions")
public class GRecordViewPredictions {

	private final GContext gContext;
	public final Variant variant;

	public GRecordViewPredictions(GContext gContext, Variant variant) {
		this.gContext = gContext;
		this.variant = variant;
	}

	@GraphQLField
	@GraphQLName("mutation_assessor_predictions")
	public List<String> getmutationAssessorPredictions() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.source, variant
		);

		return items.stream()
				.flatMap(item -> item.facets.stream())
				.flatMap(facet -> facet.transcripts.stream())
				.map(transcript -> transcript.mutationAssessorPred)
				.filter(Objects::nonNull)
				.distinct().collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("mutation_assessor_scores")
	public List<Double> getMutationAssessorScores() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.source, variant
		);

		return items.stream()
				.flatMap(item -> item.facets.stream())
				.flatMap(facet -> facet.transcripts.stream())
				.map(transcript -> transcript.mutationAssessorScore)
				.filter(Objects::nonNull)
				.distinct().collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("primate_ai_pred")
	public List<String> getPrimateAiPred() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.source, variant
		);

		return items.stream()
				.flatMap(item -> item.facets.stream())
				.map(facet -> facet.primateAiPred)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("dann_score")
	public List<Double> getDannScore() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.source, variant
		);

		return items.stream()
				.map(item -> item.dannScore)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
