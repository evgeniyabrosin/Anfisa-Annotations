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

package org.forome.annotation.processing.graphql.record.view.general.transcript;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GraphQLName("record_view_general_transcript")
public class GRecordViewGeneralTranscript {

	private final VariantVep variantVep;
	private final JSONArray jTranscripts;
	private final JSONObject jTranscript;

	public GRecordViewGeneralTranscript(VariantVep variantVep, JSONArray jTranscripts, JSONObject jTranscript) {
		this.variantVep = variantVep;
		this.jTranscripts = jTranscripts;
		this.jTranscript = jTranscript;
	}

	@GraphQLField
	@GraphQLName("id")
	public String getId() {
		return jTranscript.getAsString("transcript_id");
	}

	@GraphQLField
	@GraphQLName("gene")
	public String getGene() {
		return jTranscript.getAsString("gene_symbol");
	}

	@GraphQLField
	@GraphQLName("transcript_annotations")
	public List<String> getTranscriptAnnotation() {
		return ((JSONArray) jTranscript.get("consequence_terms")).stream().map(o -> (String) o).collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("is_worst")
	public boolean isWorst() {
		String mostSevereConsequence = variantVep.getMostSevereConsequence();

		JSONArray jTranscriptConsequenceTerms = (JSONArray) jTranscript.get("consequence_terms");

		Set<String> transcriptConsequenceTerms = (jTranscriptConsequenceTerms == null) ?
				Collections.emptySet() :
				jTranscriptConsequenceTerms.stream().map(o -> (String) o).collect(Collectors.toSet());

		boolean isMostSevere = ("protein_coding".equals(jTranscript.getAsString("biotype"))
				&& transcriptConsequenceTerms.contains(mostSevereConsequence));

		String source = jTranscript.getAsString("source");
		boolean worst = (isMostSevere && ("Ensembl".equals(source) || "RefSeq".equals(source)));

		return worst;
	}

	@GraphQLField
	@GraphQLName("is_canonical")
	public boolean isCanonical() {
		if (!"protein_coding".equals(jTranscript.getAsString("biotype"))) {
			return false;
		}

		boolean canonical = jTranscript.containsKey("canonical");

		return canonical;
	}

}
