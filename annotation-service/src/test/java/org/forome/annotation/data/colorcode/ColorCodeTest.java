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

package org.forome.annotation.data.colorcode;

import org.forome.annotation.data.anfisa.struct.ColorCode;
import org.junit.Assert;
import org.junit.Test;

public class ColorCodeTest {

	@Test
	public void test() {
		Assert.assertEquals(ColorCode.Code.YELLOW_CROSS, ColorCode.code(ColorCode.Shape.CROSS, ColorCode.Color.YELLOW));
		Assert.assertEquals(ColorCode.Code.RED_CROSS, ColorCode.code(ColorCode.Shape.CROSS, ColorCode.Color.RED));
		Assert.assertEquals(ColorCode.Code.YELLOW_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.YELLOW));
		Assert.assertEquals(ColorCode.Code.GREEN_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.GREEN));
		Assert.assertEquals(ColorCode.Code.RED_CIRCLE, ColorCode.code(ColorCode.Shape.CIRCLE, ColorCode.Color.RED));
	}
}
