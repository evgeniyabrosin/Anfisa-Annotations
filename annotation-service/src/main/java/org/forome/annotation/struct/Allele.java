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

package org.forome.annotation.struct;

import java.util.Objects;

public class Allele {

	public static Allele EMPTY = new Allele(null);

	private static String EMPTY_BASE = "-";

	private final String bases;

	public Allele(char bases) {
		this(String.valueOf(bases));
	}

	public Allele(String bases) {
		this.bases = bases;
	}

	public String getBaseString() {
		if (bases == null) {
			return EMPTY_BASE;
		} else {
			return bases;
		}
	}

	public int length() {
		if (EMPTY_BASE.equals(bases)) {
			return 0;
		} else {
			return bases.length();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		Allele allele = (Allele) o;
		return Objects.equals(bases, allele.bases);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bases);
	}
}
