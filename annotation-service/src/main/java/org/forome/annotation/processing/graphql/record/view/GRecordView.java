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
import org.forome.annotation.processing.graphql.record.view.bioinformatics.GRecordViewBioinformatics;
import org.forome.annotation.processing.graphql.record.view.general.GRecordViewGeneral;
import org.forome.annotation.processing.graphql.record.view.transcripts.GRecordViewTranscripts;
import org.forome.annotation.processing.graphql.record.view.transcripts.item.GRecordViewTranscriptsItem;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;

import java.util.List;

@GraphQLName("record_view")
public class GRecordView {

	private final GContext gContext;
	public final MCase mCase;
	public final Variant variant;

	public GRecordView(GContext gContext, MCase mCase, Variant variant) {
		this.gContext=gContext;
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
		return new GRecordViewBioinformatics(mCase, variant);
	}

//	@GraphQLField
//	@GraphQLName("transcripts")
//	public GRecordViewTranscripts getTranscripts() {
//		return new GRecordViewTranscripts(variant, gContext);
//	}


	@GraphQLField
	@GraphQLName("transcripts")
	public List<GRecordViewTranscriptsItem> getTranscripts() {
		GRecordViewTranscripts gRecordViewTranscripts = new GRecordViewTranscripts(variant, gContext);
		return gRecordViewTranscripts.getItems();
	}
}
