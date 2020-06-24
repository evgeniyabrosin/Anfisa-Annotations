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

package org.forome.annotation.data.dbnsfp.struct;

public class DbNSFPItemFacetTranscript {

	public final String mutationAssessorPred;

	public final String polyphen2HVARPred;
	public final Double polyphen2HVARScore;

	public final String polyphen2HDIVPred;
	public final Double polyphen2HDIVScore;

	public final String fATHMMPred;

	public final String siftPrediction;
	public final Double siftScore;

	public DbNSFPItemFacetTranscript(
			String mutationAssessorPred,

			String polyphen2HVARPred,
			Double polyphen2HVARScore,

			String polyphen2HDIVPred,
			Double polyphen2HDIVScore,

			String fATHMMPred,

			String siftPrediction,
			Double siftScore

	) {
		this.mutationAssessorPred = mutationAssessorPred;

		this.polyphen2HVARPred = polyphen2HVARPred;
		this.polyphen2HVARScore = polyphen2HVARScore;

		this.polyphen2HDIVPred = polyphen2HDIVPred;
		this.polyphen2HDIVScore = polyphen2HDIVScore;

		this.fATHMMPred = fATHMMPred;

		this.siftPrediction = siftPrediction;
		this.siftScore = siftScore;
	}
}
