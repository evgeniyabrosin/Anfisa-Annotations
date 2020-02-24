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

package org.forome.annotation.service.database.batchrecord.compression.impl;

import org.forome.annotation.service.database.batchrecord.compression.CompressionTest;
import org.forome.annotation.service.database.batchrecord.compression.TypeCompression;
import org.forome.annotation.service.database.batchrecord.compression.exception.NotSupportCompression;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.utils.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CompressionOrderValuesWithDictionaryTest {

	@Test
	public void testFromShortAndShort() throws NotSupportCompression {
		Class[] types = { Short.class, Short.class };

		//Generate
		List<Object[]> expected = new ArrayList<>();
		for (int index = 0; index < BatchRecord.DEFAULT_SIZE; index++) {
			Short value1 = (RandomUtils.RANDOM.nextBoolean()) ? null : CompressionTest.getRandomShort();
			Short value2 = (RandomUtils.RANDOM.nextBoolean()) ? null : CompressionTest.getRandomShort();
			expected.add(new Object[]{value1, value2});
		}

		byte[] bytes = TypeCompression.ORDER_VALUES_WITH_DICTIONARY.compression.pack(types, expected);

		//Asserts
		for (int index = 0; index < BatchRecord.DEFAULT_SIZE; index++) {
			Object[] values = TypeCompression.ORDER_VALUES_WITH_DICTIONARY.compression.unpackValues(types, bytes, 0, index);

			Assert.assertArrayEquals(expected.get(index), values);
		}

	}
}
