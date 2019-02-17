package ru.processtech.forome.annotation.connector.anfisa.struct;

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

}
