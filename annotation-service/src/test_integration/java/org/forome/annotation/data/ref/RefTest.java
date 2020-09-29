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

package org.forome.annotation.data.ref;

import org.forome.core.struct.Chromosome;
import org.junit.Assert;
import org.junit.Test;

public class RefTest extends RefBaseTest {

	@Test
	public void test() {
		Assert.assertEquals("A", refConnector.getRef(Chromosome.of("1"), 33475750, 33475750));
		Assert.assertEquals("AC", refConnector.getRef(Chromosome.of("1"), 33476224, 33476225));
		Assert.assertEquals("CAT", refConnector.getRef(Chromosome.of("1"), 103471457, 103471459));
		Assert.assertEquals("C", refConnector.getRef(Chromosome.of("10"), 123357561, 123357561));
		Assert.assertEquals("G", refConnector.getRef(Chromosome.of("11"), 727446, 727446));
		Assert.assertEquals("GCT", refConnector.getRef(Chromosome.of("11"), 832983, 832985));
	}
}
