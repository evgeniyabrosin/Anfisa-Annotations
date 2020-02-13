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
import org.forome.annotation.favor.processing.graphql.record.GRecord;
import org.forome.annotation.favor.utils.struct.table.Row;

@GraphQLName("record_view_mutation_rate")
public class GRecordViewMutationRate {

	public final Row row;

	public GRecordViewMutationRate(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_MutationDensity")
	public String getAPC_MutationDensity() {
		return GRecord.formatDouble2(row.getValue("APC.MutationDensity"));
	}

	@GraphQLField
	@GraphQLName("Freq1000bp")
	public String getFreq1000bp() {
		return GRecord.formatDouble2(row.getValue("Freq1000bp"));
	}

	@GraphQLField
	@GraphQLName("Rare1000bp")
	public String getRare1000bp() {
		return GRecord.formatDouble2(row.getValue("Rare1000bp"));
	}

	@GraphQLField
	@GraphQLName("Sngl1000bp")
	public String getSngl1000bp() {
		return GRecord.formatDouble2(row.getValue("Sngl1000bp"));
	}


}
