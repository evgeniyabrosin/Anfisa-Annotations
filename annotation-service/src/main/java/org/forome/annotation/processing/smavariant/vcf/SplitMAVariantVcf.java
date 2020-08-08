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

package org.forome.annotation.processing.smavariant.vcf;

import org.forome.annotation.data.gnomad.utils.СollapseNucleotideSequence;
import org.forome.annotation.processing.smavariant.SplitMAVariant;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantStruct;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.vcf.AlleleVCF;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.utils.variant.VariantUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SplitMAVariantVcf extends SplitMAVariant {

	private final static Logger log = LoggerFactory.getLogger(SplitMAVariantVcf.class);

	private final MAVariantVCF maVariantVCF;

	public SplitMAVariantVcf(MAVariantVCF maVariantVCF) {
		this.maVariantVCF = maVariantVCF;
	}

	@Override
	public List<Variant> split() {
		List<Variant> variants = new ArrayList<>();

		Chromosome chromosome = Chromosome.of(maVariantVCF.variantContext.getContig());
		Position position = new Position(chromosome, maVariantVCF.variantContext.getStart());
		Allele reference = new Allele(maVariantVCF.variantContext.getReference().getBaseString());

		for (Allele alt: getAlts(maVariantVCF)) {
			List<VariantVCF> iVariants = buildVariantStructs(
					position, reference, alt
			);
			variants.addAll(iVariants);
		}

		return variants;
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

	private List<VariantVCF> buildVariantStructs(Position position, Allele vcfRef, Allele vcfAlt) {
		//На этом уровне необходимо написать логику разрезание варианта и контроль, того,
		// что бы первые нуклиотидв в ref и alt - совпадали

		VariantStruct variantStruct = buildVariantStruct(position, vcfRef, vcfAlt);

		VariantVCF variantVCF = new VariantVCF(
				variantStruct.variantType,
				variantStruct.interval.start,
				variantStruct.interval.end,
				new AlleleVCF(variantStruct.ref.getBaseString(), vcfRef),
				new AlleleVCF(variantStruct.alt.getBaseString(), vcfAlt),
				variantStruct,
				maVariantVCF
		);
		variantVCF.setVepJson(maVariantVCF.getVepJson());

		return Collections.singletonList(variantVCF);
	}

	private static VariantStruct buildVariantStruct(Position position, Allele ref, Allele alt) {
		//Схлопываем аллели
		СollapseNucleotideSequence.Sequence sequence = СollapseNucleotideSequence.collapse(
				position, ref.getBaseString(), alt.getBaseString()
		);

		Allele collapseRef = new Allele(sequence.ref);
		Allele collapseAlt = new Allele(sequence.alt);

		VariantStruct variantStruct = new VariantStruct(
				VariantUtils.getVariantType(collapseRef, collapseAlt),
				buildInterval(sequence.position, collapseRef, collapseAlt),
				collapseRef, collapseAlt
		);
		return variantStruct;
	}

	private static Interval buildInterval(Position position, Allele ref, Allele alt) {
		VariantType variantType = VariantUtils.getVariantType(ref, alt);

		if (variantType == VariantType.SNV || variantType == VariantType.SUBSTITUTION) {
			return Interval.of(
					position.chromosome,
					position.value,
					position.value + ref.length() - 1
			);
		} else if (variantType == VariantType.DEL) {
			//deletion
			//TODO добавить валидацию || alt.length() > 1) - необходимо кидать ошибку
			if (ref.getBaseString().charAt(0) != ref.getBaseString().charAt(0)) {
				throw new RuntimeException("Assert fail, ref: " + ref + ", alt: " + alt);
			}
			return Interval.of(
					position.chromosome,
					position.value + 1,
					position.value + ref.length() - 1
			);
		} else if (variantType == VariantType.INS) {
			//insertion
			//TODO добавить валидацию || ref.length() > 1) - необходимо кидать ошибку
			if (ref.getBaseString().charAt(0) != ref.getBaseString().charAt(0)) {
				throw new RuntimeException("Assert fail, ref: " + ref + ", alt: " + alt);
			}
			return Interval.of(
					position.chromosome,
					position.value + 1,
					position.value
			);
		} else {
			throw new RuntimeException("not support variantType: " + variantType);
		}
	}
}
