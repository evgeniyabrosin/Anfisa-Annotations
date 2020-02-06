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

@GraphQLName("record_view_mappability")
public class GRecordViewMappability {

	public final Row row;

	public GRecordViewMappability(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_MapAbility")
	public String getAPC_MapAbility() {
		return GRecord.formatDouble2(row.getValue("APC.MapAbility"));
	}

	@GraphQLField
	@GraphQLName("Umap_k100")
	public String getUmap_k100() {
		return GRecord.formatDouble2(row.getValue("Umap_k100"));
	}

	@GraphQLField
	@GraphQLName("Bismap_k100")
	public String getBismap_k100() {
		return GRecord.formatDouble2(row.getValue("Bismap_k100"));
	}

	@GraphQLField
	@GraphQLName("Umap_k50")
	public String getUmap_k50() {
		return GRecord.formatDouble2(row.getValue("Umap_k50"));
	}

	@GraphQLField
	@GraphQLName("Bismap_k50")
	public String getBismap_k50() {
		return GRecord.formatDouble2(row.getValue("Bismap_k50"));
	}

	@GraphQLField
	@GraphQLName("Umap_k24")
	public String getUmap_k24() {
		return GRecord.formatDouble2(row.getValue("Umap_k24"));
	}

	@GraphQLField
	@GraphQLName("Bismap_k24")
	public String getBismap_k24() {
		return GRecord.formatDouble2(row.getValue("Bismap_k24"));
	}

}
