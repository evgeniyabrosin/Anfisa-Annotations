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

package org.forome.annotation.struct.variant.vep;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantStruct;
import org.forome.annotation.struct.variant.VariantType;

public abstract class VariantVep extends Variant {

	private JSONObject vepJson;

	public VariantVep(
			VariantType variantType,
			Chromosome chromosome, int start, int end,
			VariantStruct variantStruct
	) {
		super(variantType, chromosome, start, end, variantStruct);
	}

	public JSONObject getVepJson() {
		return vepJson;
	}

	public void setVepJson(JSONObject vepJson) {
		this.vepJson = vepJson;
	}

//	@Override
//	public int getStart() {
//		return vepJson.getAsNumber("start").intValue();
//	}

//	@Override
//	public VariantType getVariantType() {
//		String value = vepJson.getAsString("variant_class");
//		return VariantType.findByName(value);
//	}

	@Override
	public String getRef() {
		return vepJson.getAsString("allele_string").split("/")[0];
	}

	@Override
	public Allele getRefAllele() {
		return new Allele(getRef());
	}

	@Override
	public Allele getAlt() {
		String[] ss = vepJson.getAsString("allele_string").split("/");
		if (ss.length != 2) {
			throw new RuntimeException("Exception multi allele! " + vepJson.getAsString("allele_string"));
		}
		return new Allele(ss[1]);
	}

	public String getMostSevereConsequence() {
		return vepJson.getAsString("most_severe_consequence");
	}

	public JSONArray getTranscriptConsequences() {
		return (JSONArray) vepJson.get("transcript_consequences");
	}
}
