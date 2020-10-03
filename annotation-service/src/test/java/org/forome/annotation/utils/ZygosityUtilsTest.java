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

package org.forome.annotation.utils;


import com.google.common.collect.Lists;
import org.forome.annotation.struct.Allele;
import org.junit.Assert;
import org.junit.Test;

public class ZygosityUtilsTest {

	//chrX:32584180 G>T
	@Test
	public void testHaploid() {
		//genotype: C  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'))
		));

		//genotype: T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('T'))
		));

		//genotype: *  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'))
		));
	}


	//chr6:32584180 G>T
	@Test
	public void testDiploid() {
		//genotype: C/T  |  zygosity: 1
		Assert.assertEquals(1, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'), new Allele('T'))
		));

		//genotype: C/C  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'), new Allele('C'))
		));

		//genotype: T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('T'), new Allele('T'))
		));

		//genotype: */*  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'))
		));

		//genotype: */T  |  zygosity: 1
		Assert.assertEquals(1, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('T'))
		));
	}

	//chr1:32584180 G>T
	@Test
	public void testTriploid() {
		//genotype: C/G/A  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'), new Allele('G'), new Allele('A'))
		));

		//genotype: C/T/A  |  zygosity: 1
		Assert.assertEquals(1, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'), new Allele('T'), new Allele('A'))
		));

		//genotype: C/T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('C'), new Allele('T'), new Allele('T'))
		));

		//genotype: T/T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('T'), new Allele('T'), new Allele('T'))
		));

		//genotype: */T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('T'), new Allele('T'))
		));

		//genotype: */*/T  |  zygosity: 1
		Assert.assertEquals(1, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('T'))
		));

		//genotype: */*/*  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('*'))
		));
	}

	//chr1:32584180 G>T
	@Test
	public void testMany() {
		//genotype: */*/*/*/*  |  zygosity: 0
		Assert.assertEquals(0, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('*'), new Allele('*'), new Allele('*'))
		));

		//genotype: */*/*/*/T  |  zygosity: 1
		Assert.assertEquals(1, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('*'), new Allele('*'), new Allele('T'))
		));

		//genotype: */*/*/T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('*'), new Allele('T'), new Allele('T'))
		));

		//genotype: */*/T/T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('*'), new Allele('*'), new Allele('T'), new Allele('T'), new Allele('T'))
		));

		//genotype: T/T/T/T/T  |  zygosity: 2
		Assert.assertEquals(2, ZygosityUtils.getGenotypeZygosity(
				new Allele('T'),
				Lists.newArrayList(new Allele('T'), new Allele('T'), new Allele('T'), new Allele('T'), new Allele('T'))
		));
	}
}
