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

package org.forome.annotation.struct.variant.custom;

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.VariantStruct;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.forome.annotation.utils.variant.VariantUtils;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Interval;

public class VariantCustom extends VariantVep {

	private final Allele alt;

	public VariantCustom(Chromosome chromosome, int start, int end, Allele ref, Allele alt) {
		super(
				VariantUtils.getVariantType(ref, alt),
				chromosome,
				start, end,
				new VariantStruct(
						VariantUtils.getVariantType(ref, alt),
						Interval.of(chromosome, start, end),
						ref, alt
				)
		);
		this.alt = alt;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return null;
	}

	@Override
	public Allele getAlt() {
		return alt;
	}
}
