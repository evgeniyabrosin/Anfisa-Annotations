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

package org.forome.annotation.struct.variant;

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.mcase.Sample;

public abstract class Variant {

	public final VariantType variantType;

	public final Chromosome chromosome;

	private final int start;
	public final int end;

	public final VariantStruct variantStruct;

	public Variant(
			VariantType variantType,
			Chromosome chromosome,
			int start, int end,
			VariantStruct variantStruct
	) {
		this.variantType = variantType;
		this.chromosome = chromosome;
		this.start = start;
		this.end = end;
		this.variantStruct = variantStruct;
	}

	public final int getStart() {
		return start;
	}

	public final VariantType getVariantType() {
		return variantType;
	}

	public Genotype getGenotype(Sample sample) {
		return getGenotype(sample.id);
	}

	public abstract Genotype getGenotype(String sample);

	public abstract Allele getRefAllele();

	public abstract String getRef();

	public abstract Allele getAlt();

	public String getStrAlt() {
		return getAlt().getBaseString();
	}

	public abstract String getMostSevereConsequence();

	public Interval getInterval() {
		return Interval.of(chromosome, start, end);
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Variant{")
				.append(chromosome.getChromosome()).append(':')
				.append(start).append(' ')
				.append(getRef()).append('>')
				.append(getStrAlt());
		sBuilder.append('}');
		return sBuilder.toString();
	}
}
