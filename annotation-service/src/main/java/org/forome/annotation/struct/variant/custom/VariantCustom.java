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
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.vep.VariantVep;

public class VariantCustom extends VariantVep {

	private final int start;

	public VariantCustom(Chromosome chromosome, int start, int end) {
		super(chromosome, end);
		this.start = start;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return null;
	}

	@Override
	public Allele getRefAllele() {
		return null;
	}

	@Override
	public String getRef() {
		return null;
	}

	@Override
	public Allele getAlt() {
		return null;
	}
}
