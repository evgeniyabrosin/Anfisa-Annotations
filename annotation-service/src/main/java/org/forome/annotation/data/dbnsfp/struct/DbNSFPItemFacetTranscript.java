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

	public final String ensemblTranscriptId;

	public final String mutationAssessorPred;
	public final Double mutationAssessorScore;

	public final String polyphen2HVARPred;
	public final Double polyphen2HVARScore;

	public final String polyphen2HDIVPred;
	public final Double polyphen2HDIVScore;

	public final String fathmmPrediction;
	public final Double fathmmScore;

	public final String siftPrediction;
	public final Double siftScore;

	public final String ensemblGeneId;
	public final String ensemblProteinId;

	public final String uniprotAcc;

	public final String hgvsCAnnovar;
	public final String hgvsPAnnovar;
	public final String hgvsCSnpEff;
	public final String hgvsPSnpEff;

	public final Boolean gencodeBasic;

	public final String sift4GPrediction;
	public final Double sift4GScore;

	public final Double mpcScore;

	public DbNSFPItemFacetTranscript(
			String ensemblTranscriptId,

			String mutationAssessorPred,
			Double mutationAssessorScore,

			String polyphen2HVARPred,
			Double polyphen2HVARScore,

			String polyphen2HDIVPred,
			Double polyphen2HDIVScore,

			String fathmmPrediction,
			Double fathmmScore,

			String siftPrediction,
			Double siftScore,

			String ensemblGeneId,
			String ensemblProteinId,

			String uniprotAcc,

			String hgvsCAnnovar,
			String hgvsPAnnovar,
			String hgvsCSnpEff,
			String hgvsPSnpEff,

			Boolean gencodeBasic,

			String sift4GPrediction,
			Double sift4GScore,

			Double mpcScore
	) {
		this.ensemblTranscriptId = ensemblTranscriptId;

		this.mutationAssessorPred = mutationAssessorPred;
		this.mutationAssessorScore = mutationAssessorScore;

		this.polyphen2HVARPred = polyphen2HVARPred;
		this.polyphen2HVARScore = polyphen2HVARScore;

		this.polyphen2HDIVPred = polyphen2HDIVPred;
		this.polyphen2HDIVScore = polyphen2HDIVScore;

		this.fathmmPrediction = fathmmPrediction;
		this.fathmmScore = fathmmScore;

		this.siftPrediction = siftPrediction;
		this.siftScore = siftScore;

		this.ensemblGeneId = ensemblGeneId;
		this.ensemblProteinId = ensemblProteinId;

		this.uniprotAcc = uniprotAcc;

		this.hgvsCAnnovar = hgvsCAnnovar;
		this.hgvsPAnnovar = hgvsPAnnovar;
		this.hgvsCSnpEff = hgvsCSnpEff;
		this.hgvsPSnpEff = hgvsPSnpEff;

		this.gencodeBasic = gencodeBasic;

		this.sift4GPrediction = sift4GPrediction;
		this.sift4GScore = sift4GScore;

		this.mpcScore = mpcScore;
	}
}
