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

package org.forome.annotation.processing.graphql.record.view.bioinformatics;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.dbnsfp.struct.DbNSFPItem;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.HasVariant;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sex;
import org.forome.annotation.struct.variant.Genotype;
import org.forome.annotation.struct.variant.Variant;
import org.forome.core.struct.Chromosome;

import java.util.*;
import java.util.stream.Collectors;

@GraphQLName("record_view_bioinformatics")
public class GRecordViewBioinformatics {

	public final GContext gContext;
	public final MCase mCase;
	public final Variant variant;

	public GRecordViewBioinformatics(GContext gContext, MCase mCase, Variant variant) {
		this.gContext = gContext;

		this.variant = variant;
		this.mCase = mCase;
	}

	@GraphQLField
	@GraphQLName("zygosity")
	public String getZygosity() {
		Genotype probandGenotype = variant.getGenotype(mCase.proband);

		if (Chromosome.CHR_X.equals(variant.chromosome) && mCase.proband.sex == Sex.MALE) {
			return "X-linked";
		}

		List<Allele> alleles = probandGenotype.getAllele();
		if (alleles == null) {
			return null;
		}
		Set<Allele> uniqueAllelies = new HashSet<>(alleles);

		/**
		 * Ситуация когда ни один аллель генотипа не относится к иследуемому варианту,
		 * например ситуация, когда мы разрезали мультиалельный вариант
		 */
		if (!uniqueAllelies.contains(variant.getRefAllele()) && !uniqueAllelies.contains(variant.getAlt())) {
			return "Unknown";
		}

		HasVariant probandHasVariant = probandGenotype.getHasVariant();
		if (probandHasVariant == HasVariant.REF_REF) {
			//У пробанда нет этой мутации
			return "Homozygous Ref";
		}

		if (uniqueAllelies.size() == 1) {
			return "Homozygous";
		}
		if (uniqueAllelies.size() == 2) {
			return "Heterozygous";
		}
		return "Unknown";
	}

	@GraphQLField
	@GraphQLName("refcodon")
	public List<String> getRefcodons() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.context, variant
		);

		return items.stream()
				.flatMap(item -> item.facets.stream())
				.flatMap(facet -> facet.transcripts.stream())
				.map(transcript -> transcript.refcodon)
				.filter(Objects::nonNull)
				.flatMap(s -> Arrays.stream(s.split(";")))
				.distinct().collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("eqtl_gene")
	public List<String> getEqtlGene() {
		List<DbNSFPItem> items = gContext.anfisaConnector.dbNSFPConnector.getAll(
				gContext.context, variant
		);

		return items.stream()
				.map(item -> item.geuvadisEQtlTargetGene)
				.filter(Objects::nonNull)
				.flatMap(item -> item.stream())
				.collect(Collectors.toList());
	}

	@GraphQLField
	@GraphQLName("masked_region")
	public boolean getMaskedRegion() {
		return gContext.context.getMaskedRegion(gContext.anfisaConnector);
	}
}
