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

package org.forome.annotation.utils.bits;

import org.forome.astorage.core.utils.bits.ByteBits;
import org.junit.Assert;
import org.junit.Test;

public class ByteBitsTest {

	@Test
	public void testByUnsigned() {
		Assert.assertEquals(0, ByteBits.convertByUnsigned(Byte.MIN_VALUE));
		Assert.assertEquals(255, ByteBits.convertByUnsigned(Byte.MAX_VALUE));
	}

	@Test
	public void testFromUnsigned() {
		Assert.assertEquals(Byte.MIN_VALUE, ByteBits.convertFromUnsigned(0));
		Assert.assertEquals(Byte.MAX_VALUE, ByteBits.convertFromUnsigned(255));
	}

	@Test
	public void test() {
		for (int i = 0; i<=255; i++) {
			byte b = ByteBits.convertFromUnsigned(i);
			Assert.assertEquals(i, ByteBits.convertByUnsigned(b));
		}
	}
}
