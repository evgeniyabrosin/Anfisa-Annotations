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

package org.forome.annotation.connector.conservation;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
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
		Interval hgmdHG38_1 = liftoverConnector.toHG38(pos1);
		Conservation conservation1 = conservationConnector.getConservation(pos1, hgmdHG38_1, "G", "T");
		assertDouble(0.183d, conservation1.priPhCons);
		assertDouble(0.999d, conservation1.mamPhCons);
		assertDouble(1d, conservation1.verPhCons);
		assertDouble(0.587d, conservation1.priPhyloP);
		assertDouble(4.289d, conservation1.mamPhyloP);
		assertDouble(7.472d, conservation1.verPhyloP);
		assertFloat(4.48f, conservation1.gerpRS);//Проверяем на внешнем ресурсе
		Assert.assertEquals(null, conservation1.getGerpRSpval());
		assertFloat(4.48f, conservation1.gerpN);
		assertDouble(18.5d, conservation1.gerpS);

		//chr4:55593464 A>C rs3822214 1.82
		Chromosome chr2 = Chromosome.of("4");
		Interval pos2 = Interval.of(chr2, 55593464);
		Interval hgmdHG38_2 = liftoverConnector.toHG38(pos2);
		Conservation conservation2 = conservationConnector.getConservation(pos2, hgmdHG38_2, "A", "C");
		assertDouble(0.935d, conservation2.priPhCons);
		assertDouble(1d, conservation2.mamPhCons);
		assertDouble(0.778d, conservation2.verPhCons);
		assertDouble(0.475d, conservation2.priPhyloP);
		assertDouble(1.04d, conservation2.mamPhyloP);
		assertDouble(0.875d, conservation2.verPhyloP);
		assertFloat(1.82f, conservation2.gerpRS);//Проверяем на внешнем ресурсе
		Assert.assertEquals(null, conservation2.getGerpRSpval());
		assertFloat(5.9f, conservation2.gerpN);
		assertDouble(-36.6d, conservation2.gerpS);

		//chr9:139092481 G>C rs190916587 0.34
		Chromosome chr3 = Chromosome.of("9");
		Interval pos3 = Interval.of(chr3, 139092481);
		Interval hgmdHG38_3 = liftoverConnector.toHG38(pos3);
		Conservation conservation3 = conservationConnector.getConservation(pos3, hgmdHG38_3, "G", "C");
		assertDouble(0.99d, conservation3.priPhCons);
		assertDouble(0.993d, conservation3.mamPhCons);
		assertDouble(0.038d, conservation3.verPhCons);
		assertDouble(0.595d, conservation3.priPhyloP);
		assertDouble(-0.01d, conservation3.mamPhyloP);
		assertDouble(-0.417d, conservation3.verPhyloP);
		assertFloat(0.343f, conservation3.gerpRS);//Проверяем на внешнем ресурсе
		Assert.assertEquals(Conservation.convFromL(-232.99099978037597d), conservation3.getGerpRSpval());
		assertFloat(4.65f, conservation3.gerpN);
		assertDouble(-8.57d, conservation3.gerpS);

		//chr13:86369589 A>G rs199516562 5.86
		Chromosome chr4 = Chromosome.of("13");
		Interval pos4 = Interval.of(chr4, 86369589);
		Interval hgmdHG38_4 = liftoverConnector.toHG38(pos4);
		Conservation conservation4 = conservationConnector.getConservation(pos4, hgmdHG38_4, "A", "G");
		assertDouble(0.617d, conservation4.priPhCons);
		assertDouble(1d, conservation4.mamPhCons);
		assertDouble(1d, conservation4.verPhCons);
		assertDouble(0.475d, conservation4.priPhyloP);
		assertDouble(3.37d, conservation4.mamPhyloP);
		assertDouble(8.687d, conservation4.verPhyloP);
		assertFloat(5.86f, conservation4.gerpRS);//Проверяем на внешнем ресурсе
		Assert.assertEquals(Conservation.convFromL(-10000d), conservation4.getGerpRSpval());
		assertFloat(5.86f, conservation4.gerpN);
		assertDouble(19.8d, conservation4.gerpS);

		//chr2:21266774 GGCAGCGCCA>G rs17240441
		Chromosome chr5 = Chromosome.of("2");
		Interval pos5 = Interval.of(chr5, 21266774);
		Interval hgmdHG38_5 = liftoverConnector.toHG38(pos5);
		Conservation conservation5 = conservationConnector.getConservation(pos5, hgmdHG38_5, "GGCAGCGCCA", "G");
		Assert.assertEquals(null, conservation5);//Вычисляются только однобуквенные и инсерции

		//chr1:175949371 A>AACC
		Chromosome chr7 = Chromosome.of("1");
		Interval pos7 = Interval.of(chr7, 175949371);
		Interval hgmdHG38_7 = liftoverConnector.toHG38(pos7);
		Conservation conservation7 = conservationConnector.getConservation(pos7, hgmdHG38_7, "A", "AACC");
		assertFloat(2.37f, conservation7.gerpRS);//Проверяем на внешнем ресурсе
	}

	@Test
	public void testMulti() {
		//chr2:55863360 T>TA rs35916020
		Chromosome chr1 = Chromosome.of("2");
		Interval pos1 = Interval.of(chr1, 55863361, 55863360);
		Interval hgmdHG38_1 = liftoverConnector.toHG38(pos1);
		Conservation conservation1 = conservationConnector.getConservation(pos1, hgmdHG38_1, "T", "TA");
		assertFloat(-0.868f, conservation1.gerpRS);//Проверяем на внешнем ресурсе

		//chr1:120681278 A>AT  rs201956187
		//Проверяем ситуацию когда координаты hg38 - вычислить не удалось
		Chromosome chr2 = Chromosome.of("1");
		Interval pos2 = Interval.of(chr2, 120681279, 120681278);
		Interval hgmdHG38_2 = liftoverConnector.toHG38(pos2);
		Conservation conservation2 = conservationConnector.getConservation(pos2, hgmdHG38_2, "A", "AT");
		Assert.assertEquals(null, conservation2.priPhCons);
		Assert.assertEquals(null, conservation2.mamPhCons);
		Assert.assertEquals(null, conservation2.verPhCons);
		Assert.assertEquals(null, conservation2.priPhyloP);
		Assert.assertEquals(null, conservation2.mamPhyloP);
		Assert.assertEquals(null, conservation2.verPhyloP);
		assertFloat(0.0f, conservation2.gerpRS);//TODO необходимо подтверждение на внешнем ресурсе
		Assert.assertEquals(null, conservation2.getGerpRSpval());
		assertFloat(0.0f, conservation2.gerpN);
		Assert.assertEquals(null, conservation2.gerpS);
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
