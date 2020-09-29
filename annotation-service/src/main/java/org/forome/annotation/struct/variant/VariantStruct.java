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

package org.forome.annotation.struct.variant;

import org.forome.annotation.struct.Allele;
import org.forome.core.struct.Interval;

public class VariantStruct {

	public final VariantType variantType;
	public final Interval interval;
	public final Allele ref;
	public final Allele alt;

	public VariantStruct(VariantType variantType, Interval interval, Allele ref, Allele alt) {
		this.variantType = variantType;
		this.interval = interval;
		this.ref = ref;
		this.alt = alt;
	}



}
