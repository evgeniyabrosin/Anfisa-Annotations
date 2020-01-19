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

package org.forome.annotation.struct.variant.vcf;

import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.List;

public class VariantVCF extends VariantVep {

	public final MAVariantVCF maVariantVCF;

	private final Allele alt;

	public VariantVCF(MAVariantVCF maVariantVCF, Allele alt) {
		super(
				Chromosome.of(maVariantVCF.variantContext.getContig()),
				getEnd(maVariantVCF.variantContext)
		);
		this.maVariantVCF = maVariantVCF;

		this.alt = alt;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return new GenotypeVCF(maVariantVCF.variantContext, sample);
	}

	@Override
	public String getRef() {
		return maVariantVCF.variantContext.getReference().getBaseString();
	}

	@Override
	public Allele getAlt() {
		return alt;
	}

	/**
	 * В VCF файле могут находится аллели которые не относятся ни к одному генотипу, поэтому необходима фильтрация
	 * всех альтернативный аллелей, алгоритм следующий:
	 * Пробегаемся по всем генотипам, и посредством поля AD, суммируем как часто встречается каждый аллель в каждом генотипе
	 * В случае, если аллель встречался больше 0 раз, то мы считаем, что этот аллель используется
	 *
	 * @return
	 */
	/*
	@Override
	public List<Allele> getAlt() {
		List<Allele> alleles = variantContext.getAlleles()
				.stream().map(allele -> new Allele(allele.getBaseString())).collect(Collectors.toList());
		List<Allele> altAllels = variantContext.getAlternateAlleles()
				.stream().map(allele -> new Allele(allele.getBaseString())).collect(Collectors.toList());
		Map<Allele, Long> counts = new HashMap<>();
		for (htsjdk.variant.variantcontext.Genotype genotype : variantContext.getGenotypes()) {
			int[] ad = genotype.getAD();
			if (ad == null || ad.length == 0) {
				return altAllels;
			}
			for (int i = 0; i < alleles.size(); i++) {
				Allele al = alleles.get(i);
				long n = ad[i];
				counts.put(al, counts.getOrDefault(al, 0L) + n);
			}
		}
		List<Allele> filterAltAllels = altAllels.stream()
				.filter(s -> counts.containsKey(s) && counts.get(s) > 0)
				.collect(Collectors.toList());
		if (!filterAltAllels.isEmpty()) {
			return filterAltAllels;
		} else {
			return altAllels;
		}
	}
	*/

	public static int getStart(VariantContext variantContext) {
		if (isAnyEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
			return variantContext.getStart();
		} else {
			char fRef = variantContext.getReference().getBaseString().charAt(0);
			boolean increment = false;
			for (htsjdk.variant.variantcontext.Allele allele : variantContext.getAlternateAlleles()) {
				char fAlt = allele.getBaseString().charAt(0);
				if (fRef == fAlt) {
					increment = true;
					break;
				}
			}

			if (increment) {
				return variantContext.getStart() + 1;
			} else {
				return variantContext.getStart();
			}
		}
	}

	public static int getEnd(VariantContext variantContext) {
		return variantContext.getEnd();
	}

	/**
	 * Проверяем, что длина одного из аллелей равна длине реверенса
	 *
	 * @param reference
	 * @param alleles
	 * @return
	 */
	private static boolean isAnyEqualsLength(htsjdk.variant.variantcontext.Allele reference, List<htsjdk.variant.variantcontext.Allele> alleles) {
		for (htsjdk.variant.variantcontext.Allele allele : alleles) {
			if (allele.length() == reference.length()) {
				return true;
			}
		}
		return false;
	}

}
