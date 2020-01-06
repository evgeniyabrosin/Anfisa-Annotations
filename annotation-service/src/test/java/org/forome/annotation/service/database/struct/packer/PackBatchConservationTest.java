/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.service.database.struct.packer;

import org.apache.commons.lang3.RandomUtils;
import org.forome.annotation.connector.conservation.struct.BatchConservation;
import org.forome.annotation.connector.conservation.struct.ConservationItem;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.packbatchconservation.PackBatchConservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.junit.Assert;
import org.junit.Test;

public class PackBatchConservationTest {

	@Test
	public void test() {
		Interval interval = new Interval(Chromosome.CHR_1, 0, BatchRecord.DEFAULT_SIZE - 1);

		for (int t = 0; t < 10000; t++) {

			//generate
			ConservationItem[] expectedItems = new ConservationItem[BatchRecord.DEFAULT_SIZE];
			for (int i = 0; i < expectedItems.length; i++) {
				expectedItems[i] = new ConservationItem(
						RandomUtils.nextFloat(0.0f, 62.0f) - 31.0f,
						RandomUtils.nextFloat(0.0f, 62.0f) - 31.0f
				);
			}
			byte[] bytes = PackBatchConservation.toByteArray(new BatchConservation(interval, expectedItems));


			//restore
			ConservationItem[] actualItems = PackBatchConservation.fromByteArray(interval, bytes).items;

			//assert
			Assert.assertEquals(expectedItems.length, actualItems.length);
			for (int i = 0; i < expectedItems.length; i++) {
				ConservationItem expectedItem = expectedItems[i];
				ConservationItem actualItem = actualItems[i];

				Assert.assertEquals(expectedItem.gerpN, actualItem.gerpN, 0.001d);
				Assert.assertEquals(expectedItem.gerpRS, actualItem.gerpRS, 0.001d);
			}
		}
	}
}
