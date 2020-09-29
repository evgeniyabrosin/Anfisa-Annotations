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

package org.forome.annotation.data.conservation;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.astorage.core.data.Conservation;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Interval;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Для проверки необходимо использовать сайт
 * https://evs.gs.washington.edu/EVS/ - поле gerpRS
 */
public class ConservationTest extends AnfisaBaseTest {

	private final static Logger log = LoggerFactory.getLogger(ConservationTest.class);

	@Test
	public void testSimple() {
		//chr1:16378047 G>T rs35351345 4.48
		Chromosome chr1 = Chromosome.of("1");
		Interval pos1 = Interval.of(chr1, 16378047);
		Conservation conservation1 = conservationConnector.getConservation(Assembly.GRCh37, pos1, "G", "T");
		assertFloat(4.48f, conservation1.gerpRS);//Проверяем на внешнем ресурсе
		assertFloat(4.48f, conservation1.gerpN);

		//chr4:55593464 A>C rs3822214 1.82
		Chromosome chr2 = Chromosome.of("4");
		Interval pos2 = Interval.of(chr2, 55593464);
		Conservation conservation2 = conservationConnector.getConservation(Assembly.GRCh37, pos2, "A", "C");
		assertFloat(1.82f, conservation2.gerpRS);//Проверяем на внешнем ресурсе
		assertFloat(5.9f, conservation2.gerpN);

		//chr9:139092481 G>C rs190916587 0.34
		Chromosome chr3 = Chromosome.of("9");
		Interval pos3 = Interval.of(chr3, 139092481);
		Conservation conservation3 = conservationConnector.getConservation(Assembly.GRCh37, pos3, "G", "C");
		assertFloat(0.343f, conservation3.gerpRS);//Проверяем на внешнем ресурсе
		assertFloat(4.65f, conservation3.gerpN);

		//chr13:86369589 A>G rs199516562 5.86
		Chromosome chr4 = Chromosome.of("13");
		Interval pos4 = Interval.of(chr4, 86369589);
		Conservation conservation4 = conservationConnector.getConservation(Assembly.GRCh37, pos4, "A", "G");
		assertFloat(5.86f, conservation4.gerpRS);//Проверяем на внешнем ресурсе
		assertFloat(5.86f, conservation4.gerpN);

		//chr2:21266774 GGCAGCGCCA>G rs17240441
		Chromosome chr5 = Chromosome.of("2");
		Interval pos5 = Interval.of(chr5, 21266774);
		Conservation conservation5 = conservationConnector.getConservation(Assembly.GRCh37, pos5, "GGCAGCGCCA", "G");
		Assert.assertEquals(null, conservation5);//Вычисляются только однобуквенные и инсерции

		//chr1:175949371 A>AACC
		Chromosome chr7 = Chromosome.of("1");
		Interval pos7 = Interval.of(chr7, 175949371);
		Conservation conservation7 = conservationConnector.getConservation(Assembly.GRCh37, pos7, "A", "AACC");
		assertFloat(2.37f, conservation7.gerpRS);//Проверяем на внешнем ресурсе
	}

	@Test
	public void testMulti() {
		//chr2:55863360 T>TA rs35916020
		Chromosome chr1 = Chromosome.of("2");
		Interval pos1 = Interval.of(chr1, 55863361, 55863360);
		Conservation conservation1 = conservationConnector.getConservation(Assembly.GRCh37, pos1, "T", "TA");
		assertFloat(-0.868f, conservation1.gerpRS);//Проверяем на внешнем ресурсе

		//chr1:120681278 A>AT  rs201956187
		//Проверяем ситуацию когда координаты hg38 - вычислить не удалось
		Chromosome chr2 = Chromosome.of("1");
		Interval pos2 = Interval.of(chr2, 120681279, 120681278);
		Conservation conservation2 = conservationConnector.getConservation(Assembly.GRCh37, pos2, "A", "AT");
		Assert.assertNull(conservation2);
	}

	private void assertDouble(Double expected, Double actual) {
		if (expected != actual) {
			Assert.assertEquals((double) expected, (double) actual, 0.0000000000000001d);
		}
	}

	private void assertFloat(Float expected, Float actual) {
		if (expected != actual) {
			Assert.assertEquals((float) expected, (float) actual, 0.0000000000000001d);
		}
	}
}
