package org.forome.annotation.struct.variant.vcf;

import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariantVCF extends VariantVep {

	public final VariantContext variantContext;

	public VariantVCF(VariantContext variantContext) {
		super(
				Chromosome.of(variantContext.getContig()),
				getEnd(variantContext)
		);
		this.variantContext = variantContext;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return new GenotypeVCF(variantContext, sample);
	}

	@Override
	public String getRef() {
		return variantContext.getReference().getBaseString();
	}

	/**
	 * В VCF файле могут находится аллели которые не относятся ни к одному генотипу, поэтому необходима фильтрация
	 * всех альтернативный аллелей, алгоритм следующий:
	 * Пробегаемся по всем генотипам, и посредством поля AD, суммируем как часто встречается каждый аллель в каждом генотипе
	 * В случае, если аллель встречался больше 0 раз, то мы считаем, что этот аллель используется
	 *
	 * @return
	 */
	@Override
	public List<Allele> getAltAllele() {
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
