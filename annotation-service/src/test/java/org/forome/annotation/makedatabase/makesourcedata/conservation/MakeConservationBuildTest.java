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

package org.forome.annotation.makedatabase.makesourcedata.conservation;

import org.apache.commons.lang3.RandomUtils;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.batch.BatchRecordConservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.junit.Assert;
import org.junit.Test;

public class MakeConservationBuildTest {

	@Test
	public void test() {
		for (int k = 0; k < 100; k += 23) {

			Interval interval = Interval.of(
					Chromosome.CHR_1,
					k * BatchRecord.DEFAULT_SIZE, (k + 1) * BatchRecord.DEFAULT_SIZE - 1
			);

			for (int t = 0; t < 10000; t++) {

				//generate
				MakeConservationBuild.GerpData[] values = new MakeConservationBuild.GerpData[BatchRecord.DEFAULT_SIZE];
				for (int i = 0; i < values.length; i++) {
					values[i] = new MakeConservationBuild.GerpData(
							RandomUtils.nextFloat(0.0f, 62.0f) - 31.0f,
							RandomUtils.nextFloat(0.0f, 62.0f) - 31.0f
					);
				}
				MakeConservationBuild makeConservationBuild = new MakeConservationBuild(interval, values);
				byte[] bytes = makeConservationBuild.build();

				//restore
				BatchRecordConservation batchRecordConservation = new BatchRecordConservation(interval, bytes, 0);

				//assert
				for (int p = interval.start; p < interval.end; p++) {
					Position position = new Position(interval.chromosome, p);

					Assert.assertEquals(values[p - interval.start].gerpN, batchRecordConservation.getGerpN(position), 0.001d);
					Assert.assertEquals(values[p - interval.start].gerpRS, batchRecordConservation.getGerpRS(position), 0.001d);
				}
			}
		}


	}
}
