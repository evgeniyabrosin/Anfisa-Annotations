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
import org.forome.annotation.struct.HasVariant;
import org.forome.annotation.struct.variant.Genotype;

import java.util.ArrayList;
import java.util.List;

public class GenotypeVCF extends Genotype {

	private final VariantVCF variantVCF;

	private final htsjdk.variant.variantcontext.Genotype vcfGenotype;

	public GenotypeVCF(VariantVCF variantVCF, VariantContext variantContext, String sampleName) {
		super(sampleName);
		this.variantVCF = variantVCF;
		this.vcfGenotype = variantContext.getGenotype(sampleName);
	}

	@Override
	public HasVariant getHasVariant() {
		//REF - референс
		//ALT - альтернативный аллель
		//ALTk - альтернативный аллель не относящийся к обрабатываемому single варианту
		//Например:
		//chr1:100818464 T>C,A
		//В первом single вариант: REF - T, ALT - C, ALTk - A
		//Во втором single вариант: REF - T, ALT - A, ALTk - C

		// ALTk/ALTk: 0
		// REF/REF: 0

		// REF/ALT: 1
		// ALT/REF: 1

		// ALT/ALTk: 2
		// ALTk/ALT: 2
		switch (vcfGenotype.getType()) {
			case NO_CALL: //Генотип не может быть определен из-за плохого качества секвенирования
			case UNAVAILABLE: //Не имеет альтернативных аллелей
			case MIXED:
				return HasVariant.MIXED;
			case HOM_REF:
			case HET:
			case HOM_VAR:
				//Звездочка означает мусор. Считаем, что звездочка – это референс

				AlleleVCF ref = (AlleleVCF) variantVCF.getRefAllele();
				String sourceRef = ref.vcfSource.getBaseString();//Изначальную последовательность

				AlleleVCF alt = (AlleleVCF) variantVCF.getAlt();
				String sourceAlt = alt.vcfSource.getBaseString();//Изначальную последовательность

				String allele1 = vcfGenotype.getAlleles().get(0).getBaseString();
				boolean isRef1 = "*".equals(allele1) || sourceRef.equals(allele1);

				if (vcfGenotype.getAlleles().size()==1) {
					//У haploid'ых хромосом только одна алеля, например у хромосомы X
					if (isRef1) {
						// REF/REF
						return HasVariant.REF_REF;
					} else if (!allele1.equals(sourceAlt)) {
						// ALTk/ALTk: 0
						return HasVariant.ALTki_ALTkj;
					} else {
						// ALT/ALTk, ALTk/ALT
						return HasVariant.ALT_ALTki;
					}
				} else {
					String allele2 = vcfGenotype.getAlleles().get(1).getBaseString();
					boolean isRef2 = "*".equals(allele2) || sourceRef.equals(allele2);
					if (isRef1 && isRef2) {
						// REF/REF
						return HasVariant.REF_REF;
					} else if (!allele1.equals(sourceAlt) && !allele2.equals(sourceAlt)) {
						// ALTk/ALTk: 0
						return HasVariant.ALTki_ALTkj;
					} else if (!isRef1 && !isRef2) {
						// ALT/ALTk, ALTk/ALT
						return HasVariant.ALT_ALTki;
					} else {
						// REF/ALT, ALT/REF
						return HasVariant.REF_ALT;
					}
				}
			default:
				throw new RuntimeException("Unknown state: " + vcfGenotype.getType());
		}
	}

	@Override
	public List<Allele> getAllele() {
		if (vcfGenotype.isCalled()) {
			final List<Allele> al = new ArrayList<Allele>();
			for (htsjdk.variant.variantcontext.Allele a : vcfGenotype.getAlleles()) {
				al.add(new Allele(a.getBaseString()));
			}
			return al;
		} else {
			return null;
		}
	}

	@Override
	public Integer getGQ() {
		int value = vcfGenotype.getGQ();
		return (value != -1) ? value : null;
	}

}
