package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONObject;

public class AnfisaResult {

	public final AnfisaResultFilters filters;
	public final AnfisaResultData data;
	public final AnfisaResultView view;
	public final String recordType;

	public AnfisaResult(
			AnfisaResultFilters filters,
			AnfisaResultData data,
			AnfisaResultView view
	) {
		this.filters = filters;
		this.data = data;
		this.view = view;
		this.recordType = "variant";
	}

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();
		out.put("_filters", filters.toJSON(data, view.bioinformatics));
		out.put("data", data.toJSON());
		out.put("view", view.toJSON());
		out.put("record_type", recordType);
		return out;
	}
}
