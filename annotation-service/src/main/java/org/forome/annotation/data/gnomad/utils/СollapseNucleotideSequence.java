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

package org.forome.annotation.data.gnomad.utils;

import org.forome.core.struct.Position;

public class СollapseNucleotideSequence {

	public static class Sequence {

		public final Position position;
		public final String ref;
		public final String alt;

		public Sequence(Position position, String ref, String alt) {
			this.position = position;
			this.ref = ref;
			this.alt = alt;
		}
	}

	public static Sequence collapse(Position position, String ref, String alt) {
		Sequence sequenceCollapseRight = collapseRight(position, ref, alt);
		return collapseLeft(
				sequenceCollapseRight.position,
				sequenceCollapseRight.ref,
				sequenceCollapseRight.alt
		);
	}

	/**
	 * Схлопываем последовательность (справа), т.е.
	 * при: ref: TGGAGGAGGA, alt: TGGA
	 * получаем: ref: TGGAGGA, alt: T
	 */
	public static Sequence collapseRight(Position position, String ref, String alt) {
		int rShift = 0;
		while (true) {
			if (ref.length() - rShift == 1) break;
			if (alt.length() - rShift == 1) break;
			if (ref.charAt(ref.length() - rShift - 1) != alt.charAt(alt.length() - rShift - 1)) break;
			rShift++;
		}

		return new Sequence(
				position,
				ref.substring(0, ref.length() - rShift),
				alt.substring(0, alt.length() - rShift)
		);
	}

	/**
	 * Схлопываем последовательность (слева), т.е.
	 * при: position: 119999968, ref: AAAGAAAGA, alt: AAAGAAAGG
	 * получаем: position: 119999976, ref: A, alt: G
	 * -----
	 * при: position: 148670533, ref: AAAAAAA, alt: AAT
	 * получаем: position: 148670534, ref: AAAAAA, alt: AT
	 */
	public static Sequence collapseLeft(Position position, String ref, String alt) {
		int lShift = 0;
		while (true) {
			if (ref.length() - lShift == 1) break;
			if (alt.length() - lShift == 1) break;
			if (ref.charAt(lShift) != alt.charAt(lShift)) break;
			lShift++;
		}
		if (ref.length() - lShift != 1 || alt.length() - lShift != 1) {
			//Если не snv, то первые буквы у alt и ref должны совпадать,
			// поэтому если надо отатываем одну позицию назад
			if (ref.charAt(lShift) != alt.charAt(lShift) && lShift > 0) {
				lShift--;
			}
		}

		return new Sequence(
				new Position(position.chromosome, position.value + lShift),
				ref.substring(lShift),
				alt.substring(lShift)
		);
	}
}
