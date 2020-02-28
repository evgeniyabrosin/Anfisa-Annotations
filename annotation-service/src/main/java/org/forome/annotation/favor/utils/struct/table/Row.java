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

package org.forome.annotation.favor.utils.struct.table;

import java.util.Collections;
import java.util.List;

public class Row {

	public final int order;

	public final Table table;
	public final List<String> values;

	public final String rawLine;

	public Row(int order, Table table, List<String> values, String rawLine) {
		this.order = order;

		this.table = table;
		this.values = Collections.unmodifiableList(values);
		this.rawLine = rawLine;
	}

	public String getValue(Field field) {
		int index = table.fields.indexOf(field);
		return values.get(index);
	}

	public String getValue(String field) {
		return getValue(new Field(field));
	}

}
