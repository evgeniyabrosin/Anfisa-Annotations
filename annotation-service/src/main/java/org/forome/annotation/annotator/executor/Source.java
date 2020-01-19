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

package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.mavariant.MAVariantVep;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

class Source {

	private final static Logger log = LoggerFactory.getLogger(Source.class);

	public final MAVariantVep variant;
	public final JSONObject vepJson;

	public Source(MAVariantVep variant, JSONObject vepJson) {
		this.variant = variant;
		this.vepJson = vepJson;

		variant.setVepJson(vepJson);

		//Валидация на соотвествие строк
		if (variant instanceof MAVariantVCF) {
			VariantContext variantContext = ((MAVariantVCF) variant).variantContext;

			String[] vepJsonInput = vepJson.getAsString("input").split("\t");

			String vcfChromosome = RequestParser.toChromosome(variantContext.getContig());
			String vepJsonChromosome = RequestParser.toChromosome(vepJsonInput[0]);
			if (!vcfChromosome.equals(vepJsonChromosome)) {
				throw new RuntimeException(
						String.format("Not equals chromosome, vcf %s and vep.json %s", vcfChromosome, vepJsonChromosome)
				);
			}

			String vcfId = variantContext.getID();
			String vepJsonId = vepJsonInput[2];
			if (!vcfId.equals(vepJsonId)) {
				throw new RuntimeException(
						String.format("Not equals id, vcf %s and vep.json %s", vcfId, vepJsonId)
				);
			}

			//Валидация позиций
			if (!Objects.equals(
					Chromosome.of(variantContext.getContig()),
					Chromosome.of(vepJson.getAsString("seq_region_name"))
			)) {
				throw new RuntimeException(
						String.format("Not equals chromosome, vcf %s and vep.json %s", variantContext.getContig(), vepJson)
				);
			}
//			if (VariantVCF.getStart(variantContext) != vepJson.getAsNumber("start").intValue()) {
//				throw new RuntimeException(
//						String.format("Not equals start, vcf: %s, vep.json: %s, input: %s",
//								VariantVCF.getStart(variantContext), vepJson.getAsNumber("start"),
//								vepJson.getAsString("input")
//						)
//				);
//			}
			if (VariantVCF.getEnd(variantContext) != vepJson.getAsNumber("end").intValue()) {
				throw new RuntimeException(
						String.format("Not equals end, vcf: %s, vep.json: %s, input: %s",
								VariantVCF.getEnd(variantContext), vepJson.getAsNumber("end"),
								vepJson.getAsString("input")
						)
				);
			}

		}
	}
}
