package ru.processtech.forome.annotation.connector.clinvar.struct;

import java.util.Map;

public class ClinvarResult {

	public final long start;
	public final long end;
	public final String referenceAllele;
	public final String alternateAllele;
	public final String variationID;
	public final String clinicalSignificance;
	public final String phenotypeIDs;
	public final String otherIDs;
	public final String phenotypeList;

	public final Map<String, String> submitters;

	public ClinvarResult(
			long start,
			long end,
			String referenceAllele,
			String alternateAllele,
			String variationID,
			String clinicalSignificance,
			String phenotypeIDs,
			String otherIDs,
			String phenotypeList,
			Map<String, String> submitters
	) {
		this.start = start;
		this.end = end;
		this.referenceAllele = referenceAllele;
		this.alternateAllele = alternateAllele;
		this.variationID = variationID;
		this.clinicalSignificance = clinicalSignificance;
		this.phenotypeIDs = phenotypeIDs;
		this.otherIDs = otherIDs;
		this.phenotypeList = phenotypeList;

		this.submitters = submitters;
	}
}
