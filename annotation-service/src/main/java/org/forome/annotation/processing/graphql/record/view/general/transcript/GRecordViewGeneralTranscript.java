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

import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("record_view_general_transcript")
public class GRecordViewGeneralTranscript {

	private final VariantVep variantVep;
	protected final JSONObject jTranscript;

	protected final String biotype;
	protected final String source;

	public GRecordViewGeneralTranscript(VariantVep variantVep, JSONObject jTranscript) {
		this.variantVep = variantVep;
		this.jTranscript = jTranscript;

		this.biotype = jTranscript.getAsString("biotype");
		this.source = jTranscript.getAsString("source");
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
	@GraphQLName("is_worst")
	public boolean isWorst() {
		if (!"protein_coding".equals(biotype)) {
			return false;
		}

		JSONArray jTranscriptConsequenceTerms = (JSONArray) jTranscript.get("consequence_terms");
		if (jTranscriptConsequenceTerms == null) {
			return false;
		}

		String mostSevereConsequence = variantVep.getMostSevereConsequence();
		boolean isMostSevere = jTranscriptConsequenceTerms.stream()
				.map(o -> (String) o).filter(s -> s.equals(mostSevereConsequence)).findFirst().isPresent();

		return (isMostSevere && ("Ensembl".equals(source) || "RefSeq".equals(source)));
	}

	@GraphQLField
	@GraphQLName("is_canonical")
	public boolean isCanonical() {
		if (!"protein_coding".equals(biotype)) {
			return false;
		}

		return jTranscript.containsKey("canonical");
	}

	@GraphQLField
	@GraphQLName("transcript_annotations")
	public List<String> getTranscriptAnnotation() {
		return ((JSONArray) jTranscript.get("consequence_terms")).stream().map(o -> (String) o).collect(Collectors.toList());
	}

}
