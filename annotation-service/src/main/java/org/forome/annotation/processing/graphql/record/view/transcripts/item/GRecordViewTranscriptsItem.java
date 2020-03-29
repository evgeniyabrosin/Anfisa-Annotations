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

package org.forome.annotation.processing.graphql.record.view.transcripts.item;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONObject;
import org.forome.annotation.processing.graphql.record.view.general.transcript.GRecordViewGeneralTranscript;
import org.forome.annotation.struct.variant.vep.VariantVep;

@GraphQLName("record_view_transcripts_item")
public class GRecordViewTranscriptsItem extends GRecordViewGeneralTranscript {

	public GRecordViewTranscriptsItem(VariantVep variantVep, JSONObject jTranscript) {
		super(variantVep, jTranscript);
	}

	@GraphQLField
	@GraphQLName("biotype")
	public String getBioType() {
		return biotype;
	}

	@GraphQLField
	@GraphQLName("codons")
	public String getCodons() {
		return jTranscript.getAsString("codons");
	}

	@GraphQLField
	@GraphQLName("amino_acids")
	public String getAminoAcids() {
		return jTranscript.getAsString("amino_acids");
	}


	@GraphQLField
	@GraphQLName("transcript_source")
	public String getSource() {
		return source;
	}

//	@GraphQLField
//	@GraphQLName("cpos")
//	public String getCPos() {
//		return jTranscript.getAsString("amino_acids");
//	}

}
