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

package org.forome.annotation.processing.graphql.record.data;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.processing.utils.OutUtils;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.List;

@GraphQLName("record_data")
public class GRecordData {

	private final GContext gContext;
	private final Variant variant;

	public GRecordData(GContext gContext, Variant variant) {
		this.gContext = gContext;
		this.variant = variant;
	}


	@GraphQLField
	@GraphQLName("label")
	public String getLabel() {
		List<String> genes = AnfisaConnector.getGenes((VariantVep)variant);
		String gene;
		if (genes.size() == 0) {
			gene = "None";
		} else if (genes.size() < 3) {
			gene = String.join(",", genes);
		} else {
			gene = "...";
		}

		return String.format("[%s] %s", gene, OutUtils.toOut(variant));
	}




}
