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

package org.forome.annotation.struct.sequence;

import org.forome.annotation.struct.nucleotide.Nucleotide;
import org.forome.core.struct.Interval;

public class Sequence {

	public final Interval interval;
	private final Nucleotide[] nucleotides;

	public Sequence(Interval interval, Nucleotide[] nucleotides) {
		if (interval.end - interval.start + 1 != nucleotides.length) {
			throw new IllegalArgumentException();
		}

		this.interval = interval;
		this.nucleotides = nucleotides;
	}

	public String getValue() {
		StringBuilder sBuilder = new StringBuilder(nucleotides.length);
		for (int i = 0; i < nucleotides.length; i++) {
			sBuilder.append(nucleotides[i].character);
		}
		return sBuilder.toString();
	}

	public static Sequence build(Interval interval, String value) {
		Nucleotide[] nucleotides = new Nucleotide[value.length()];
		for (int i = 0; i < value.length(); i++) {
			nucleotides[i] = Nucleotide.of(value.charAt(i));
		}
		return new Sequence(interval, nucleotides);
	}

}
