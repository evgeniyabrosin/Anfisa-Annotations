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

package org.forome.annotation.processing.smavariant;

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.mavariant.MAVariant;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SplitMAVariant {

	private static final Logger log = LoggerFactory.getLogger(SplitMAVariant.class);

	public static List<Variant> split(MAVariant maVariant) {
		try {
			List<Variant> variants = new ArrayList<>();
			if (maVariant instanceof MAVariantVCF) {
				MAVariantVCF maVariantVCF = (MAVariantVCF) maVariant;
				for (Allele alt : getAlts(maVariantVCF)) {
					VariantVCF variantVCF = new VariantVCF(maVariantVCF, alt);
					variantVCF.setVepJson(maVariantVCF.getVepJson());
					variants.add(variantVCF);
				}
			} else {
				throw new RuntimeException();
			}
			return variants;
		} catch (Throwable e) {
			throw new RuntimeException("Exception build variant: " + maVariant.toString(), e);
		}
	}


	/**
	 * В VCF файле могут находится аллели которые не относятся ни к одному генотипу, поэтому необходима фильтрация
	 * всех альтернативный аллелей, алгоритм следующий:
	 * Пробегаемся по всем генотипам, и посредством поля AD, суммируем как часто встречается каждый аллель в каждом генотипе
	 * В случае, если аллель встречался больше 0 раз, то мы считаем, что этот аллель используется
	 *
	 * @return
	 */
	private static List<Allele> getAlts(MAVariantVCF maVariant) {
		htsjdk.variant.variantcontext.VariantContext variantContext = maVariant.variantContext;
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
				if (al.getBaseString().trim().length() == 0) {
					continue;
				}

				//Иногда встречаются vcf-файлы в которых ad не соотвествует аллелям
				if (ad.length <= i) {
					log.warn("Bad vcf format: AD genotype: {}, variant: {}", genotype, maVariant);
					continue;
				}

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
}

