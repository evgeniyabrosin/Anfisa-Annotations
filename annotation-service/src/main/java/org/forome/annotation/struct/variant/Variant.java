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
import org.forome.annotation.struct.mcase.Sample;

public abstract class Variant {

	public final Chromosome chromosome;
	public final int end;

	public Variant(Chromosome chromosome, int end) {
		this.chromosome = chromosome;
		this.end = end;
	}

	public abstract int getStart();

	public abstract VariantType getVariantType();

	public Genotype getGenotype(Sample sample) {
		return getGenotype(sample.id);
	}

	public abstract Genotype getGenotype(String sample);

	public abstract String getId();

	public abstract Allele getRefAllele();

	public abstract String getRef();

	public abstract Allele getAlt();

	public String getStrAlt() {
		return getAlt().getBaseString();
	}

	public abstract String getMostSevereConsequence();

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Variant{")
				.append(chromosome.getChromosome()).append(':')
				.append(getStart()).append(' ')
				.append(getRef()).append('>')
				.append(getStrAlt());
		if (getId() != null) {
			sBuilder.append(" (").append(getId()).append(')');
		}
		sBuilder.append('}');
		return sBuilder.toString();
	}
}
