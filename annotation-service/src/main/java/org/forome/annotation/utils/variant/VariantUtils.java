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

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.variant.VariantType;

public class VariantUtils {

	public static VariantType getVariantType(Allele ref, Allele alt) {
		if (ref.length() == alt.length() && ref.length() == 1) {
			return VariantType.SNV;
		} else if (alt.length() == 1 && ref.length() > alt.length()
				&& alt.getBaseString().charAt(0) == ref.getBaseString().charAt(0)
		) {
			return VariantType.DEL;
		} else if (ref.length() == 1 && ref.length() < alt.length()
				&& alt.getBaseString().charAt(0) == ref.getBaseString().charAt(0)
		) {
			return VariantType.INS;
		} else {
			return VariantType.SUBSTITUTION;
		}
	}
}
