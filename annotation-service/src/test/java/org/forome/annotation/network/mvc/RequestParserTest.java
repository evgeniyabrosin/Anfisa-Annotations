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

package org.forome.annotation.network.mvc;

import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.exception.AnnotatorException;
import org.junit.Assert;
import org.junit.Test;

public class RequestParserTest {

	@Test
	public void test() {
		Assert.assertEquals("1", RequestParser.toChromosome("1"));
		Assert.assertEquals("1", RequestParser.toChromosome("chr1"));
		Assert.assertEquals("5", RequestParser.toChromosome("5"));
		Assert.assertEquals("5", RequestParser.toChromosome("chr5"));
		Assert.assertEquals("23", RequestParser.toChromosome("23"));
		Assert.assertEquals("23", RequestParser.toChromosome("chr23"));
		Assert.assertEquals("X", RequestParser.toChromosome("X"));
		Assert.assertEquals("Y", RequestParser.toChromosome("Y"));

		try {
			RequestParser.toChromosome("");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("0");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("24");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("chr0");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("chr24");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("h");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}

		try {
			RequestParser.toChromosome("A");
			Assert.fail();
		} catch (AnnotatorException ignore) {
		}
	}
}
