package org.forome.annotation.connector.clinvar.struct;

public class Row {

	public final long start;
	public final long end;
	public final String type;
	public final String referenceAllele;
	public final String alternateAllele;
	public final String rcvAccession;
	public final String variationID;
	public final String clinicalSignificance;
	public final String phenotypeIDs;
	public final String otherIDs;
	public final String phenotypeList;

	public Row(
			long start,
			long end,
			String type,
			String referenceAllele,
			String alternateAllele,
			String rcvAccession,
			String variationID,
			String clinicalSignificance,
			String phenotypeIDs,
			String otherIDs,
			String phenotypeList
	) {
		this.start = start;
		this.end = end;
		this.type = type;
		this.referenceAllele = referenceAllele;
		this.alternateAllele = alternateAllele;
		this.rcvAccession = rcvAccession;
		this.variationID = variationID;
		this.clinicalSignificance = clinicalSignificance;
		this.phenotypeIDs = phenotypeIDs;
		this.otherIDs = otherIDs;
		this.phenotypeList = phenotypeList;
	}
}
