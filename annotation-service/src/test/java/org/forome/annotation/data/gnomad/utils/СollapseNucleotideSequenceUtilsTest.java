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

import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.junit.Assert;
import org.junit.Test;

public class СollapseNucleotideSequenceUtilsTest {

	@Test
	public void testRight() {
		Position position1 = new Position(Chromosome.CHR_2, 73613032);
		String ref1 = "TGGAGGAGGA";
		String alt1 = "TGGA";

		СollapseNucleotideSequence.Sequence result = СollapseNucleotideSequence.collapseRight(
				position1, ref1, alt1
		);
		Assert.assertEquals(position1, result.position);
		Assert.assertEquals("TGGAGGA", result.ref);
		Assert.assertEquals("T", result.alt);
	}

	@Test
	public void testSNVRight() {
		Position position1 = new Position(Chromosome.CHR_2, 73613032);
		String ref1 = "G";
		String alt1 = "T";

		СollapseNucleotideSequence.Sequence result = СollapseNucleotideSequence.collapseRight(
				position1, ref1, alt1
		);
		Assert.assertEquals(position1, result.position);
		Assert.assertEquals(ref1, result.ref);
		Assert.assertEquals(alt1, result.alt);
	}

	@Test
	public void testLeftSNV() {
		Position position1 = new Position(Chromosome.CHR_2, 119999968);
		String ref1 = "AAAGAAAGA";
		String alt1 = "AAAGAAAGG";

		СollapseNucleotideSequence.Sequence result = СollapseNucleotideSequence.collapseLeft(
				position1, ref1, alt1
		);
		Assert.assertEquals(119999976, result.position.value);
		Assert.assertEquals("A", result.ref);
		Assert.assertEquals("G", result.alt);
	}

	@Test
	public void testLeftSubstitution() {
		Position position1 = new Position(Chromosome.CHR_1, 148670533);
		String ref1 = "AAAAAAA";
		String alt1 = "AAT";

		СollapseNucleotideSequence.Sequence result = СollapseNucleotideSequence.collapseLeft(
				position1, ref1, alt1
		);
		Assert.assertEquals(148670534, result.position.value);
		Assert.assertEquals("AAAAAA", result.ref);
		Assert.assertEquals("AT", result.alt);
	}

	@Test
	public void test(){
		Position position1 = new Position(Chromosome.CHR_2, 119999968);
		String ref1 = "AAAGAAAGAGGA";
		String alt1 = "AAAGAAAGGGGA";

		СollapseNucleotideSequence.Sequence result = СollapseNucleotideSequence.collapse(
				position1, ref1, alt1
		);
		Assert.assertEquals(119999976, result.position.value);
		Assert.assertEquals("A", result.ref);
		Assert.assertEquals("G", result.alt);
	}
}
