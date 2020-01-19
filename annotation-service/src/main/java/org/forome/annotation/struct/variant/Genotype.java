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

import java.util.List;
import java.util.stream.Collectors;

public abstract class Genotype {

	public final String sampleName;

	public Genotype(String sampleName) {
		this.sampleName = sampleName;
	}

	public abstract int hasVariant();

	public abstract List<Allele> getAllele();

	public String getGenotypeString() {
		List<Allele> alleles = getAllele();
		if (alleles == null) {
			return null;
		} else {
			return alleles.stream().map(allele -> allele.getBaseString()).collect(Collectors.joining("/"));
		}
	}

	public abstract Integer getGQ();

}
