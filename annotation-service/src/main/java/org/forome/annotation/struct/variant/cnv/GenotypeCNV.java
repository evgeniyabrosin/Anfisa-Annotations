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

package org.forome.annotation.struct.variant.cnv;

import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.HasVariant;
import org.forome.annotation.struct.variant.Genotype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenotypeCNV extends Genotype {

	private VariantCNV variantCNV;

	/**
	 * 0/0 – означает, что сэмпл гомозиготен по референсу (то есть, у него вариант отсутствует),
	 * 0/1 – это гетерозигота, то есть, имеется один альтернативный аллель,
	 * 1/1 – гомозигота, то есть, оба аллеля – альтернативные
	 */
	public final String gt;

	/**
	 * Оценка надежности данной записи
	 */
	public final float lo;

	public GenotypeCNV(String sampleName, String gt, float lo) {
		super(sampleName);
		this.gt = gt;
		this.lo = lo;
	}

	protected void setVariantCNV(VariantCNV variantCNV) {
		this.variantCNV = variantCNV;
	}

	@Override
	public HasVariant getHasVariant() {
		int[] gts = Arrays.stream(gt.split("/")).map(s -> Integer.parseInt(s)).mapToInt(Integer::intValue).toArray();
		if (gts[0] == 0 && gts[1] == 0) {
			return HasVariant.REF_REF;
		} else if (gts[0] == 0 || gts[1] == 0) {
			return HasVariant.REF_ALT;
		} else {
			return HasVariant.ALT_ALTki;
		}
	}

	@Override
	public List<Allele> getAllele() {
		final List<Allele> al = new ArrayList<Allele>();
		al.add(new Allele(variantCNV.getRef()));

		int[] gts = Arrays.stream(gt.split("/")).map(s -> Integer.parseInt(s)).mapToInt(Integer::intValue).toArray();
		al.add(new Allele(variantCNV.getAllele(gts[1])));

		return al;
	}

	/**
	 * Зиготность равна количеству аллелей, соответсвующих альтенативной аллели
	 * @return
	 */
	@Override
	public int getZygosity() {
		long count = Arrays.stream(gt.split("/"))
				.map(s -> Integer.parseInt(s)).mapToInt(Integer::intValue)
				.filter(value -> value == 1)
				.count();
		return (count > 2L) ? 2 : (int) count;
	}

	@Override
	public Integer getGQ() {
		return null;
	}

}
