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

import org.forome.annotation.struct.Position;

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
}
