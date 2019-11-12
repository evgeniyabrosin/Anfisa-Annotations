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

package org.forome.annotation.connector.conservation.struct;

import org.junit.Assert;
import org.junit.Test;

public class ConservationTest {

	@Test
	public void test() {
		String expecteds[] = {"1.53166e-289", "1.00000e-29", "1.53166e-9", "0"};
		for (String expected: expecteds) {
			Double value = Conservation.convToL(expected);
			String actual = Conservation.convFromL(value);
			Assert.assertEquals(expected, actual);
		}
	}
}

