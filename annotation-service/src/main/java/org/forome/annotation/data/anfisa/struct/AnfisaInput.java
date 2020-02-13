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

package org.forome.annotation.data.anfisa.struct;

import org.forome.annotation.struct.mcase.MCase;

import java.util.Collections;
import java.util.LinkedHashMap;

public class AnfisaInput {

	public final MCase mCase;

	private AnfisaInput(MCase mCase) {
		if (mCase == null) new IllegalArgumentException();
		this.mCase = mCase;
	}

	public static class Builder {

		private MCase mCase;

		public Builder() {
			this.mCase = new MCase.Builder(new LinkedHashMap<>(), Collections.emptyList()).build();
		}

		public Builder withSamples(MCase mCase) {
			this.mCase = mCase;
			return this;
		}

		public AnfisaInput build() {
			return new AnfisaInput(
					mCase
			);
		}
	}
}
