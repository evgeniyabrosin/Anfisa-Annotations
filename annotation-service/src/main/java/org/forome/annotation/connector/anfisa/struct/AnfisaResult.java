/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONObject;

public class AnfisaResult {

	public final AnfisaResultFilters filters;
	public final AnfisaResultData data;
	public final AnfisaResultView view;

	public AnfisaResult(
			AnfisaResultFilters filters,
			AnfisaResultData data,
			AnfisaResultView view
	) {
		this.filters = filters;
		this.data = data;
		this.view = view;
	}

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();
		out.put("_view", view.toJSON());
		out.put("_filters", filters.toJSON(data, view.databases, view.bioinformatics));
		out.put("__data", data.toJSON());
		return out;
	}
}
