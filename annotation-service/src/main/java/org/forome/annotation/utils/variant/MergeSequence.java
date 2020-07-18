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

import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Sequence;
import org.forome.annotation.struct.variant.Variant;

public class MergeSequence {

	private Sequence sequence;

	public MergeSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	public String merge(Variant variant) {
		int i1;
		int i2;
		if (variant.getRefAllele().length() == variant.getAlt().length()) {
			//snv
			i1 = variant.getStart() - sequence.interval.start;
			i2 = i1 + variant.getRefAllele().length();
		} else if (variant.getRefAllele().length() > variant.getAlt().length()) {
			//deletion
			i1 = variant.getStart() - sequence.interval.start - 1;
			i2 = i1 + variant.getRefAllele().length();
		} else {
			//insertion
			i1 = variant.getStart() - sequence.interval.start - 1;
			i2 = i1 + variant.getRefAllele().length();
		}

		String s1 = sequence.value.substring(0, i1);
		String change = sequence.value.substring(i1, i2);
		String s2 = sequence.value.substring(i2);

		//TODO Ulitin V. Нашлась интересная ситуация, кейс: pgp3140_wgs_nist-v4.2
		//Варинант chr1    147380845       .       T       G,TG
		//Из за того, что логика вычисления страрт отличается в snv и в инсерции, а позиция у нас одна, то нельзя
		//Вычислять start по отдельности, иначе у нас произойдет ошибка как в настоящий момент
		//валидацию первый варианта(snv) прошел успешо и референс был правильный, но следующая инсерция свалилась
		// по валидации референса, т.к. мы сделали смещение, но как оказывается в этой ситуации этого было делать нельзя
		if (!change.equalsIgnoreCase(variant.getRef())) {
			throw ExceptionBuilder.buildInvalidSequenceReference(variant, change);
		}

		return s1 + variant.getAlt().getBaseString() + s2;
	}
}
