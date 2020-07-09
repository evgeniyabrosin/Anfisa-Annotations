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

package org.forome.annotation.processing.graphql.record;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.schema.DataFetchingEnvironment;
import org.forome.annotation.processing.graphql.record.data.GRecordData;
import org.forome.annotation.processing.graphql.record.filters.GRecordFilters;
import org.forome.annotation.processing.graphql.record.view.GRecordView;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;

@GraphQLName("query")
public class GRecord {

	@GraphQLField
	@GraphQLName("_filters")
	public static GRecordFilters getGRecordFilters(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		Variant variant = gContext.variant;
		MCase mCase = gContext.mCase;
		return new GRecordFilters(gContext, mCase, variant);
	}

	@GraphQLField
	@GraphQLName("_view")
	public static GRecordView getGRecordView(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		Variant variant = gContext.variant;
		MCase mCase = gContext.mCase;
		return new GRecordView(gContext, mCase, variant);
	}

	@GraphQLField
	@GraphQLName("__data")
	public static GRecordData getGRecordData(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		Variant variant = gContext.variant;
		return new GRecordData(gContext, variant);
	}

	@GraphQLField
	@GraphQLName("record_type")
	public static String getRecordType() {
		return "variant";
	}
}

