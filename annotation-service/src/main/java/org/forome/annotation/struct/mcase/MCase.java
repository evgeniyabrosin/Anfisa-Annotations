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

package org.forome.annotation.struct.mcase;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class MCase {

	public final Sample proband;
	public final Map<String, Sample> samples;
	public final List<Cohort> cohorts;

	private MCase(Sample proband, Map<String, Sample> samples, List<Cohort> cohorts) {
		this.proband = proband;
		this.samples = Collections.unmodifiableMap(samples);
		this.cohorts = Collections.unmodifiableList(cohorts);
	}

	public static class Builder {

		private SortedMap<String, Sample> samples;
		private List<Cohort> cohorts;

		public Builder(SortedMap<String, Sample> samples, List<Cohort> cohorts) {
			this.samples = samples;
			this.cohorts = cohorts;
		}

		public MCase build() {
			return new MCase(getProband(), samples, cohorts);
		}

		private Sample getProband() {
			if (samples == null || samples.isEmpty()) {
				return null;
			}
			Sample proband = samples.get(samples.firstKey());

			//Validation
			for (Map.Entry<String, Sample> entry : samples.entrySet()) {
				if (entry.getValue() == proband) continue;
				if (entry.getValue().id.endsWith("a1")) {
					throw new RuntimeException("Not valid samples, a1 is not first record");
				}
			}

			return proband;
		}

	}
}
