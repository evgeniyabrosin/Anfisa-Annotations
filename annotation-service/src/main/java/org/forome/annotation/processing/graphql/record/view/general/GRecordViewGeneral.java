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

package org.forome.annotation.processing.graphql.record.view.general;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.processing.graphql.record.view.general.transcript.GRecordViewGeneralTranscript;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.processing.utils.OutUtils;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GraphQLName("record_view_general")
public class GRecordViewGeneral {

	public final GContext gContext;
	public final Variant variant;

	public GRecordViewGeneral(GContext gContext, Variant variant) {
		this.gContext = gContext;
		this.variant = variant;
	}


	@GraphQLField
	@GraphQLName("hg19")
	public String getHg19() {
		Assembly assembly = gContext.context.anfisaInput.mCase.assembly;
		if (assembly == Assembly.GRCh37) {
			return OutUtils.toOut(variant);
		} else {
			Interval interval37 = gContext.anfisaConnector.liftoverConnector.toHG37(
					assembly,
					variant.getInterval()
			);
			return (interval37 != null) ? OutUtils.toOut(interval37) : "None";
		}
	}


	@GraphQLField
	@GraphQLName("hg38")
	public String getHg38() {
		Assembly assembly = gContext.context.anfisaInput.mCase.assembly;
		if (assembly == Assembly.GRCh38) {
			return OutUtils.toOut(variant);
		} else {
			Interval interval38 = gContext.anfisaConnector.liftoverConnector.toHG38(
					assembly,
					variant.getInterval()
			);
			return (interval38 != null) ? OutUtils.toOut(interval38) : "None";
		}
	}


	@GraphQLField
	@GraphQLName("transcripts")
	public List<GRecordViewGeneralTranscript> getTranscripts() {
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

			List<GRecordViewGeneralTranscript> transcripts = new ArrayList<>();
			for (JSONObject jTranscript : jTranscripts) {
				String transcriptId = jTranscript.getAsString("transcript_id");

				transcripts.add(new GRecordViewGeneralTranscript(
						transcriptId, variantVep, jTranscript
				));
			}
			return transcripts;
		} else {
			return null;
		}
	}

}
