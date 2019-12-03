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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cohort {

	public final String name;

	private List<Sample> samples;

	public Cohort(String name) {
		this.name = name;
		this.samples = new ArrayList<>();
	}

	protected void register(Sample sample) {
		samples.add(sample);
	}

	public List<Sample> getSamples() {
		return Collections.unmodifiableList(samples);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Cohort cohort = (Cohort) o;
		return Objects.equals(name, cohort.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
