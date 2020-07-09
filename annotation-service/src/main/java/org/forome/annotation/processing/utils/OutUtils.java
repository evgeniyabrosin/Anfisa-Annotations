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

package org.forome.annotation.processing.utils;

import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;

public class OutUtils {

	public static String toOut(Interval interval) {
		if (interval.isSingle()) {
			return String.format("%s:%s", interval.chromosome.getChromosome(), interval.start);
		} else {
			return String.format("%s:%s-%s", interval.chromosome.getChromosome(), interval.start, interval.end);
		}
	}

	public static String toOut(Interval interval, String ref, String alt) {
		return String.format("%s %s>%s", toOut(interval), ref, alt);
	}

	public static String toOut(Variant variant) {
		VariantType variantType = variant.getVariantType();
		Interval interval = variant.getInterval();

		if (variantType == VariantType.SNV) {
			return String.format("%s %s>%s", toOut(interval), variant.getRef(), variant.getStrAlt());
		} else {
			return String.format("%s %s", toOut(interval), (variantType != null) ? variantType.toJSON() : "None");
		}
	}

}
