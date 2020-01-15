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

package org.forome.annotation.data.liftover;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.junit.Assert;
import org.junit.Test;

public class LiftoverTest extends AnfisaBaseTest {

	@Test
	public void testFail() {
		//chr1:120681278 A>AT  rs201956187
		/**
		 * Это случай, когда программа liftOver работает не правильно. Причем ее веб версия дает ошибку
		 * (https://genome.ucsc.edu/cgi-bin/hgLiftOver дает #Split in new
		 * chr1:120681278-120681279 - Sequence insufficiently intersects multiple chains),
		 * а утилита просто молча выдает ахинею. Причина понятна, как раз в этом месте принципиально поменялся способ,
		 * которым собирается референс. Как это отлавливать не знаю.
		 * Сам dbSNP показывает этот вариант в 38-й сборке, как chr1:120138709-120138721, что тоже странно,
		 * поскольку он больше двух нуклеотидов не занимает (https://www.ncbi.nlm.nih.gov/snp/rs201956187),
		 * отуда берется 12 позиций я не понимаю.
		 * ==================================
		 * Если длина мутации первышает 10 позиций (base pairs), то мы такие мутации переводить в 38-ю сборку не будем
		 */
		Assert.assertEquals(null, liftoverConnector.toHG38(
				Interval.of(Chromosome.of("1"), 120681279, 120681278)
		));

	}
}
