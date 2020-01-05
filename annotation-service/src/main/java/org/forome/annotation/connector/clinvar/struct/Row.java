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

package org.forome.annotation.connector.clinvar.struct;

public class Row {

	public final int start;
	public final int end;
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
			int start,
			int end,
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
