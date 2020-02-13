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

@GraphQLName("record_view_local_nucleotide_diversity")
public class GRecordViewLocalNucleotideDiversity {

	public final Row row;

	public GRecordViewLocalNucleotideDiversity(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_Local_Nucleotide_Diversity")
	public String getAPC_Local_Nucleotide_Diversity() {
		return GRecord.formatDouble2(row.getValue("APC.Local.Diversity"));
	}

	@GraphQLField
	@GraphQLName("RecRate")
	public String getRecRate() {
		return GRecord.formatDouble2(row.getValue("RecRate"));
	}

	@GraphQLField
	@GraphQLName("NucDiv")
	public String getNucDiv() {
		return GRecord.formatDouble2(row.getValue("NucDiv"));
	}

	@GraphQLField
	@GraphQLName("bStatistics")
	public String getbStatistics() {
		return GRecord.formatDouble2(row.getValue("bStatistics"));
	}


}
