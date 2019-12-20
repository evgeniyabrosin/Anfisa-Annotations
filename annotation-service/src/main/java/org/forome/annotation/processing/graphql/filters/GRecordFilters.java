/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.processing.graphql.filters;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import org.forome.annotation.connector.anfisa.struct.AnfisaVariant;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.utils.MathUtils;

@GraphQLName("record_filters")
public class GRecordFilters {

	public final MCase mCase;
	public final Variant variant;

	public GRecordFilters(MCase mCase, Variant variant) {
		this.mCase = mCase;
		this.variant = variant;
	}

	@GraphQLField
	@GraphQLName("chromosome")
	public String getChromosome() {
		return variant.chromosome.getChromosome();
	}

	@GraphQLField
	@GraphQLName("fs")
	public double getFS() {
		if (variant instanceof VariantVCF) {
			VariantContext variantContext = ((VariantVCF) variant).variantContext;
			CommonInfo commonInfo = variantContext.getCommonInfo();
			return MathUtils.toPrimitiveDouble(commonInfo.getAttribute("FS"));
		} else {
			return 0;
		}
	}

	@GraphQLField
	@GraphQLName("qd")
	public double getQD() {
		if (variant instanceof VariantVCF) {
			VariantContext variantContext = ((VariantVCF) variant).variantContext;
			CommonInfo commonInfo = variantContext.getCommonInfo();
			return MathUtils.toPrimitiveDouble(commonInfo.getAttribute("QD"));
		} else {
			return 0;
		}
	}

	@GraphQLField
	@GraphQLName("mq")
	public double getMQ() {
		if (variant instanceof VariantVCF) {
			VariantContext variantContext = ((VariantVCF) variant).variantContext;
			CommonInfo commonInfo = variantContext.getCommonInfo();
			return MathUtils.toPrimitiveDouble(commonInfo.getAttribute("MQ"));
		} else {
			return 0;
		}
	}

	@GraphQLField
	@GraphQLName("min_gq")
	public Integer getMinGQ() {
		if (mCase == null) {
			return null;
		}

		Integer GQ = null;
		for (Sample sample : mCase.samples.values()) {
			Integer gq = variant.getGenotype(sample).getGQ();
			if (gq != null && gq != 0) {
				if (GQ == null || gq < GQ) {
					GQ = gq;
				}
			}
		}
		return GQ;
	}


	@GraphQLField
	@GraphQLName("severity")
	public Long getSeverity() {
		String csq = variant.getMostSevereConsequence();
		int n = AnfisaVariant.SEVERITY.size();
		for (int s = 0; s < n; s++) {
			if (AnfisaVariant.SEVERITY.get(s).contains(csq)) {
				return Long.valueOf(n - s - 2);
			}
		}
		return null;
	}


}
