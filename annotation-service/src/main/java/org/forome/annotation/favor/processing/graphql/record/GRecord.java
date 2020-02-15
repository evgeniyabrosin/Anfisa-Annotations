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

package org.forome.annotation.favor.processing.graphql.record;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.schema.DataFetchingEnvironment;
import org.forome.annotation.favor.processing.graphql.record.data.GRecordData;
import org.forome.annotation.favor.processing.graphql.record.filters.GRecordFilters;
import org.forome.annotation.favor.processing.graphql.record.view.GRecordView;
import org.forome.annotation.favor.processing.struct.GContext;
import org.forome.annotation.favor.utils.struct.table.Row;

import java.util.Locale;

@GraphQLName("query")
public class GRecord {

	@GraphQLField
	@GraphQLName("_filters")
	public static GRecordFilters getGRecordFilters(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		return new GRecordFilters(gContext);
	}

	@GraphQLField
	@GraphQLName("_view")
	public static GRecordView getGRecordView(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		return new GRecordView(gContext);
	}

	@GraphQLField
	@GraphQLName("__data")
	public static GRecordData getGRecordData(DataFetchingEnvironment env) {
		GContext gContext = env.getContext();
		Row row = gContext.row;
		return new GRecordData(row);
	}

	@GraphQLField
	@GraphQLName("record_type")
	public static String getRecordType() {
		return "variant";
	}

	public static String formatDouble2(String value) {
		if (value == null) return null;
		return String.format(Locale.ENGLISH, "%.2f", Double.parseDouble(value));
	}

	public static Double toDouble(String value) {
		if (value == null) return null;
		return Double.parseDouble(value);
	}
}

