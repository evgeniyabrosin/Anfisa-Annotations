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

import java.util.List;

@GraphQLName("record_view_general_transcript")
public class GRecordViewGeneralTranscript {

	public final String id;
	public final String gene;
	public final List<String> transcriptAnnotations;

	public GRecordViewGeneralTranscript(
			String id,
			String gene,
			List<String> transcriptAnnotations
	) {
		this.id = id;
		this.gene = gene;
		this.transcriptAnnotations = transcriptAnnotations;
	}

	@GraphQLField
	@GraphQLName("id")
	public String getId() {
		return id;
	}

	@GraphQLField
	@GraphQLName("gene")
	public String getGene() {
		return gene;
	}

	@GraphQLField
	@GraphQLName("transcript_annotations")
	public List<String> getTranscriptAnnotation() {
		return transcriptAnnotations;
	}

}
