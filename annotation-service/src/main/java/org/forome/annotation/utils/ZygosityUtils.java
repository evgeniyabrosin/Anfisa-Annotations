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

package org.forome.annotation.utils;

import org.forome.annotation.struct.Allele;

import java.util.List;

public class ZygosityUtils {

	/**
	 * Most of chromosomes for human are pairwise, so a mutation/variant applying to a sample has one of the following states:
	 * Zygosity 0: variant is absent
	 * Zygosity 1: variant is heterozygous, it presents on one chromosome copy
	 * Zygosity 2: variant is homozygous, it presents on both copies of chromosome
	 * <p>
	 * Males have chromosomes X and Y in one copies, so a mutation/variant on these chromosomes for male sample has one of the following states:
	 * Zygosity 0: variant is absent
	 * Zygosity 2: variant is hemozygous, it does present on single copy of chromosome
	 *
	 * @param alt
	 * @param genotypeAlleles
	 * @return
	 * @see <a href="https://foromeplatform.github.io/anfisa/zygosity.html">https://foromeplatform.github.io/anfisa/zygosity.html</a>
	 */
	public static int getGenotypeZygosity(Allele alt, List<Allele> genotypeAlleles) {
		if (genotypeAlleles.size() == 1) {
			//Haploid
			return (genotypeAlleles.get(0).equalsIgnoreConservative(alt)) ? 2 : 0;
		} else {
			long count = genotypeAlleles.stream()
					.filter(allele -> allele.equalsIgnoreConservative(alt))
					.count();
			return (count > 2L) ? 2 : (int) count;
		}
	}
}
