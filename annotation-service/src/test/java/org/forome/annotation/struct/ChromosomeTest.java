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

package org.forome.annotation.struct;

import org.forome.annotation.exception.AnnotatorException;
import org.junit.Assert;
import org.junit.Test;

public class ChromosomeTest {

	@Test
	public void test() {
		Assert.assertEquals("1", Chromosome.of("1").getChar());
		Assert.assertEquals("1", Chromosome.of("chr1").getChar());
		Assert.assertEquals("5", Chromosome.of("5").getChar());
		Assert.assertEquals("5", Chromosome.of("chr5").getChar());
		Assert.assertEquals("23", Chromosome.of("23").getChar());
		Assert.assertEquals("23", Chromosome.of("chr23").getChar());
		Assert.assertEquals("X", Chromosome.of("X").getChar());
		Assert.assertEquals("Y", Chromosome.of("Y").getChar());

		try {
			Chromosome.of("");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

	}
}
