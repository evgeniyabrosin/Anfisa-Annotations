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

package org.forome.annotation.processing.graphql.record.view.facets;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItemFacet;


@GraphQLName("record_view_facet")
public class GRecordViewFacet {

	private final DbNSFPItemFacet facet;

	public GRecordViewFacet(DbNSFPItemFacet facet) {
		this.facet = facet;
	}

	@GraphQLField
	@GraphQLName("refcodon")
	public String getRefcodon() {
		return facet.refcodon;
	}

	@GraphQLField
	@GraphQLName("codonpos")
	public String getCodonpos() {
		return facet.codonpos;
	}


}
