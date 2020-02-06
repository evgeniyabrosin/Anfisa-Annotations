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

package org.forome.annotation.favor.processing.graphql.record.view;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.favor.utils.struct.table.Row;

@GraphQLName("record_view")
public class GRecordView {

	public final Row row;

	public GRecordView(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("general")
	public GRecordViewGeneral getGRecordViewGeneral() {
		return new GRecordViewGeneral(row);
	}

	@GraphQLField
	@GraphQLName("variant_category")
	public GRecordViewVariantCategory getVariantCategory() {
		return new GRecordViewVariantCategory(row);
	}

	@GraphQLField
	@GraphQLName("allele_frequencies")
	public GRecordViewAlleleFrequencies getAlleleFrequencies() {
		return new GRecordViewAlleleFrequencies(row);
	}

	@GraphQLField
	@GraphQLName("integrative_score")
	public GRecordViewIntegrativeScore getIntegrativeScore() {
		return new GRecordViewIntegrativeScore(row);
	}

	@GraphQLField
	@GraphQLName("protein_function")
	public GRecordViewProteinFunction getProteinFunction() {
		return new GRecordViewProteinFunction(row);
	}

	@GraphQLField
	@GraphQLName("conservation")
	public GRecordViewConservation getConservation() {
		return new GRecordViewConservation(row);
	}

	@GraphQLField
	@GraphQLName("epigenetics")
	public GRecordViewEpigenetics[] getEpigenetics() {
		return new GRecordViewEpigenetics[] {
				new GRecordViewEpigenetics(GRecordViewEpigenetics.Mode.STATUS, row),
				new GRecordViewEpigenetics(GRecordViewEpigenetics.Mode.VALUE, row)
		};
	}

	@GraphQLField
	@GraphQLName("transcription_factors")
	public GRecordViewTranscriptionFactors getTranscriptionFactors() {
		return new GRecordViewTranscriptionFactors(row);
	}

	@GraphQLField
	@GraphQLName("chromatin_states")
	public GRecordViewChromatinStates getChromatinStates() {
		return new GRecordViewChromatinStates(row);
	}

	@GraphQLField
	@GraphQLName("local_nucleotide_diversity")
	public GRecordViewLocalNucleotideDiversity getLocalNucleotideDiversity() {
		return new GRecordViewLocalNucleotideDiversity(row);
	}

	@GraphQLField
	@GraphQLName("mutation_rate")
	public GRecordViewMutationRate getMutationRate() {
		return new GRecordViewMutationRate(row);
	}

	@GraphQLField
	@GraphQLName("mappability")
	public GRecordViewMappability getMappability() {
		return new GRecordViewMappability(row);
	}

}
