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

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.VariantStruct;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.forome.core.struct.Chromosome;

public class VariantVCF extends VariantVep {

	public final boolean isSplitting;
	public final MAVariantVCF maVariantVCF;

	public final AlleleVCF ref;
	public final AlleleVCF alt;

	public VariantVCF(
			VariantType variantType,
			int start, int end,
			AlleleVCF ref, AlleleVCF alt,
			VariantStruct variantStruct,
			boolean isSplitting,
			MAVariantVCF maVariantVCF
	) {
		super(
				variantType,
				Chromosome.of(maVariantVCF.variantContext.getContig()),
				start, end,
				variantStruct
		);

		this.isSplitting = isSplitting;
		this.maVariantVCF = maVariantVCF;

		this.ref = ref;
		this.alt = alt;
	}

	@Override
	public Genotype getGenotype(String sample) {
		return new GenotypeVCF(this, maVariantVCF.variantContext, sample);
	}

	@Override
	public Allele getRefAllele() {
		return ref;
	}

	@Override
	public String getRef() {
		return getRefAllele().getBaseString();
	}

	@Override
	public Allele getAlt() {
		return alt;
	}

//	public static int getStart(VariantContext variantContext) {
//		if (isAnyEqualsLength(variantContext.getReference(), variantContext.getAlternateAlleles())) {
//			return variantContext.getStart();
//		} else {
//			char fRef = variantContext.getReference().getBaseString().charAt(0);
//			boolean increment = false;
//			for (htsjdk.variant.variantcontext.Allele allele : variantContext.getAlternateAlleles()) {
//				char fAlt = allele.getBaseString().charAt(0);
//				if (fRef == fAlt) {
//					increment = true;
//					break;
//				}
//			}
//
//			if (increment) {
//				return variantContext.getStart() + 1;
//			} else {
//				return variantContext.getStart();
//			}
//		}
//	}
//
//	public static int getEnd(VariantContext variantContext) {
//		return variantContext.getEnd();
//	}

	/**
	 * Проверяем, что длина одного из аллелей равна длине реверенса
	 *
	 * @param reference
	 * @param alleles
	 * @return
	 */
//	private static boolean isAnyEqualsLength(htsjdk.variant.variantcontext.Allele reference, List<htsjdk.variant.variantcontext.Allele> alleles) {
//		for (htsjdk.variant.variantcontext.Allele allele : alleles) {
//			if (allele.length() == reference.length()) {
//				return true;
//			}
//		}
//		return false;
//	}

}
