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

@GraphQLName("record_view_variant_category")
public class GRecordViewVariantCategory {

	public final Row row;

	public GRecordViewVariantCategory(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("GENCODE_Category")
	public String getGENCODE_Category() {
		return row.getValue("GENCODE.Category");
	}

	@GraphQLField
	@GraphQLName("GENCODE_Info")
	public String getGENCODE_Info() {
		return row.getValue("GENCODE.Info");
	}

	@GraphQLField
	@GraphQLName("GENCODE_Exonic_Category")
	public String getGENCODE_Exonic_Category() {
		return row.getValue("GENCODE.EXONIC.Category");
	}

	@GraphQLField
	@GraphQLName("GENCODE_Exonic_Info")
	public String getGENCODE_Exonic_Info() {
		return row.getValue("GENCODE.EXONIC.Info");
	}

	@GraphQLField
	@GraphQLName("Disruptive_Missense")
	public String getDisruptive_Missense() {
		return row.getValue("lof.in.nonsynonymous");
	}

	@GraphQLField
	@GraphQLName("CAGE_Promoter")
	public String getCAGE_Promoter() {
		return row.getValue("CAGE.Promoter");
	}

	@GraphQLField
	@GraphQLName("CAGE_Enhancer")
	public String getCAGE_Enhancer() {
		return row.getValue("CAGE.Enhancer");
	}

	@GraphQLField
	@GraphQLName("GeneHancer")
	public String getGeneHancer() {
		return row.getValue("GeneHancer");
	}

	@GraphQLField
	@GraphQLName("SuperEnhancer")
	public String getSuperEnhancer() {
		return row.getValue("SuperEnhancer");
	}

	@GraphQLField
	@GraphQLName("ClinVar")
	public String getClinVar() {
		return row.getValue("clinvar");
	}

}
