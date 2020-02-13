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

@GraphQLName("record_view_conservation")
public class GRecordViewConservation {

	public final Row row;

	public GRecordViewConservation(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_Conservation")
	public String getAPC_Conservation() {
		return GRecord.formatDouble2(row.getValue("APC.Conservation"));
	}

	@GraphQLField
	@GraphQLName("priPhCons")
	public String getpriPhCons() {
		return GRecord.formatDouble2(row.getValue("priPhCons"));
	}

	@GraphQLField
	@GraphQLName("mamPhCons")
	public String getmamPhCons() {
		return GRecord.formatDouble2(row.getValue("mamPhCons"));
	}

	@GraphQLField
	@GraphQLName("verPhCons")
	public String getverPhCons() {
		return GRecord.formatDouble2(row.getValue("verPhCons"));
	}

	@GraphQLField
	@GraphQLName("priPhyloP")
	public String getpriPhyloP() {
		return GRecord.formatDouble2(row.getValue("priPhyloP"));
	}

	@GraphQLField
	@GraphQLName("mamPhyloP")
	public String getmamPhyloP() {
		return GRecord.formatDouble2(row.getValue("mamPhyloP"));
	}

	@GraphQLField
	@GraphQLName("verPhyloP")
	public String getverPhyloP() {
		return GRecord.formatDouble2(row.getValue("verPhyloP"));
	}

	@GraphQLField
	@GraphQLName("GerpN")
	public String getGerpN() {
		return GRecord.formatDouble2(row.getValue("GerpN"));
	}

	@GraphQLField
	@GraphQLName("GerpS")
	public String getGerpS() {
		return GRecord.formatDouble2(row.getValue("GerpS"));
	}

}
