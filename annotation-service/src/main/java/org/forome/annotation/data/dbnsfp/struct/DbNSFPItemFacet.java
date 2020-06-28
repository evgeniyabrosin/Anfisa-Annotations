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

import java.util.Collections;
import java.util.List;

public class DbNSFPItemFacet {

	public final Double revelScore;
	public final Double siftConvertedRankScore;

	public final Double metaLRScore;
	public final Double metaLRRankScore;
	public final String metaLRPrediction;

	public final String refcodon;
	public final String codonpos;

	public final List<DbNSFPItemFacetTranscript> transcripts;

	public DbNSFPItemFacet(
			Double revelScore,
			Double siftConvertedRankScore,

			Double metaLRScore,
			Double metaLRRankScore,
			String metaLRPrediction,

			String refcodon,
			String codonpos,

			List<DbNSFPItemFacetTranscript> transcripts
	) {
		this.revelScore = revelScore;
		this.siftConvertedRankScore = siftConvertedRankScore;

		this.metaLRScore = metaLRScore;
		this.metaLRRankScore = metaLRRankScore;
		this.metaLRPrediction = metaLRPrediction;

		this.refcodon = refcodon;
		this.codonpos = codonpos;

		this.transcripts = Collections.unmodifiableList(transcripts);
	}
}
