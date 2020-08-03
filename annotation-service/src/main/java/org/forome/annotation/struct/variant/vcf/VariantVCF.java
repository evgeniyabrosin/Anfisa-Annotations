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

	private final AlleleVCF ref;
	private final AlleleVCF alt;

	public VariantVCF(MAVariantVCF maVariantVCF, AlleleVCF alt) {
		super(
				Chromosome.of(maVariantVCF.variantContext.getContig()),
				getEnd(maVariantVCF.variantContext)
		);
		this.maVariantVCF = maVariantVCF;

		String refBaseString = maVariantVCF.variantContext.getReference().getBaseString();
		this.ref = new AlleleVCF(refBaseString, new Allele(refBaseString));
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
