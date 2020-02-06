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

@GraphQLName("record_view_allele_frequencies")
public class GRecordViewAlleleFrequencies {

	public final Row row;

	public GRecordViewAlleleFrequencies(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("TOPMed_Bravo_AF")
	public String getTOPMed_Bravo_AF() {
		return row.getValue("Bravo_AF");
	}

	@GraphQLField
	@GraphQLName("GNOMAD_Total_AF")
	public String getGNOMAD_Total_AF() {
		return row.getValue("GNOMAD_total");
	}

	@GraphQLField
	@GraphQLName("ALL_1000G_AF")
	public String getALL_1000G_AF() {
		return row.getValue("TG_ALL");
	}

	@GraphQLField
	@GraphQLName("EUR_1000G_AF")
	public String getEUR_1000G_AF() {
		return row.getValue("TG_EUR");
	}

	@GraphQLField
	@GraphQLName("AFR_1000G_AF")
	public String getAFR_1000G_AF() {
		return row.getValue("TG_AFR");
	}

	@GraphQLField
	@GraphQLName("AMR_1000G_AF")
	public String getAMR_1000G_AF() {
		return row.getValue("TG_AMR");
	}

	@GraphQLField
	@GraphQLName("SAS_1000G_AF")
	public String getSAS_1000G_AF() {
		return row.getValue("TG_SAS");
	}

	@GraphQLField
	@GraphQLName("EAS_1000G_AF")
	public String getEAS_1000G_AF() {
		return row.getValue("TG_EAS");
	}

	@GraphQLField
	@GraphQLName("ExAC03")
	public String getExAC03() {
		return row.getValue("ExAC03");
	}

	@GraphQLField
	@GraphQLName("ExAC03_nontcga")
	public String getExAC03_nontcga() {
		return row.getValue("ExAC03_nontcga");
	}

	@GraphQLField
	@GraphQLName("ESP_ALL")
	public String getESP_ALL() {
		return row.getValue("ESP_ALL");
	}

	@GraphQLField
	@GraphQLName("ESP_EA")
	public String getESP_EA() {
		return row.getValue("ESP_EA");
	}

	@GraphQLField
	@GraphQLName("ESP_AA")
	public String getESP_AA() {
		return row.getValue("ESP_AA");
	}
}
