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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariantCNV extends VariantVep {

	public static final String COPY_NUMBER_VARIATION = "copy_number_variation";

	public final List<String> exonNums;

	public final Map<String, GenotypeCNV> genotypes;
	public final List<String> transcripts;

	public VariantCNV(Chromosome chromosome, int start, int end, List<String> exonNums, List<String> transcripts, List<GenotypeCNV> genotypes) {
		super(chromosome, start, end);
		genotypes.forEach(genotypeCNV -> genotypeCNV.setVariantCNV(this));
		this.exonNums = exonNums;
		this.transcripts = Collections.unmodifiableList(transcripts);
		this.genotypes = Collections.unmodifiableMap(
				genotypes.stream()
						.collect(Collectors.toMap(item -> item.sampleName, item -> item))
		);
	}

	@Override
	public GenotypeCNV getGenotype(String sample) {
		return genotypes.get(sample);
	}

	@Override
	public VariantType getVariantType() {
		return VariantType.CNV;
	}

	@Override
	public String getRef() {
		return "[LONG REF]";
	}

	@Override
	public List<Allele> getAltAllele() {
		return Collections.singletonList(Allele.EMPTY);
	}

	protected String getAllele(int index) {
		if (index == 0) {
			return getRef();
		} else {
			return getStrAltAllele().get(index - 1);
		}
	}

	@Override
	public String getMostSevereConsequence() {
		return COPY_NUMBER_VARIATION;
	}

	@Override
	public String getId() {
		return new StringBuilder()
				.append(chromosome.getChar()).append('_')
				.append(start).append('_')
				.append(getRef()).append("/-")
				.toString();
	}

	@Override
	public JSONArray getTranscriptConsequences() {
		JSONArray result = new JSONArray();
		for (Object o : super.getTranscriptConsequences()) {
			JSONObject item = (JSONObject) o;
			if (!transcripts.contains(item.getAsString("transcript_id"))) {
				continue;
			}
			if (!"protein_coding".equals(item.getAsString("biotype"))) {
				continue;
			}

			item.remove("given_ref");
			item.remove("used_ref");
			item.put("consequence_terms", new JSONArray() {{
				add(COPY_NUMBER_VARIATION);
			}});

			result.add(item);
		}
		return result;
	}
}
