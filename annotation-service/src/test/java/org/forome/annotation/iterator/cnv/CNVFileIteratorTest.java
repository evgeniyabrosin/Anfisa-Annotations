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

package org.forome.annotation.iterator.cnv;

import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.mavariant.MAVariantCNV;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CNVFileIteratorTest {

	@Test
	public void test() throws URISyntaxException {
		Path pathCnv = Paths.get(getClass().getClassLoader().getResource("cnv/deletions.svaba.exons.txt").toURI());
		CNVFileIterator cnvFileIterator = new CNVFileIterator(pathCnv);

		MAVariantCNV variant1 = cnvFileIterator.next();
		Assert.assertNotNull(variant1);
		Assert.assertEquals(Chromosome.of("9"), variant1.variantCNV.chromosome);
		Assert.assertEquals(140772677, variant1.variantCNV.getStart());
		Assert.assertEquals(140777187, variant1.variantCNV.end);


		MAVariantCNV variant2 = cnvFileIterator.next();
		Assert.assertNotNull(variant2);
		Assert.assertEquals(Chromosome.of("9"), variant2.variantCNV.chromosome);
		Assert.assertEquals(140772688, variant2.variantCNV.getStart());
		Assert.assertEquals(140777198, variant2.variantCNV.end);


		MAVariantCNV variant3 = cnvFileIterator.next();
		Assert.assertNotNull(variant3);
		Assert.assertEquals(Chromosome.of("10"), variant3.variantCNV.chromosome);
		Assert.assertEquals(140772688, variant3.variantCNV.getStart());
		Assert.assertEquals(140777198, variant3.variantCNV.end);


		//Проверяем, что больше вариантов нет
		Assert.assertFalse(cnvFileIterator.hasNext());
	}
}


