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

public class DbNSFPItem {

	public final Double caddRaw;
	public final Double caddPhred;

	public final Double dannScore;

	public final String mutationTasterPred;

	public final String primateAiPred;

	public final List<String> geuvadisEQtlTargetGene;

	public final List<DbNSFPItemFacet> facets;

	public DbNSFPItem(
			Double caddRaw, Double caddPhred,
			Double dannScore,
			String mutationTasterPred,
			String primateAiPred,
			List<String> geuvadisEQtlTargetGene,
			List<DbNSFPItemFacet> facets
	) {
		this.caddRaw = caddRaw;
		this.caddPhred = caddPhred;
		this.dannScore = dannScore;
		this.mutationTasterPred = mutationTasterPred;
		this.primateAiPred = primateAiPred;
		this.geuvadisEQtlTargetGene = geuvadisEQtlTargetGene;
		this.facets = Collections.unmodifiableList(facets);
	}
}
