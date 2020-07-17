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

package org.forome.annotation.utils.variant;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Sequence;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.junit.Assert;
import org.junit.Test;

public class MergeSequenceTest {

	@Test
	public void testDeletion() {
		Sequence sequence = new Sequence(
				Interval.of(Chromosome.of("2"), 73448096, 73448105),
				"TTCTCCTCTA"
		);

		VariantCustom variant = new VariantCustom(
				Chromosome.of("2"), 73448098, 73448100, new Allele("T")
		);
		variant.setVepJson(new JSONObject(){{
			put("allele_string", "TCTC/T");
		}});

		Assert.assertEquals("TTCTCTA", new MergeSequence(sequence).merge(variant));
	}
}
