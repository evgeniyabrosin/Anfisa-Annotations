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
