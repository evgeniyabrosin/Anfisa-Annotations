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
	public void test() throws Exception {
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
	public void testSNV() {
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
}
