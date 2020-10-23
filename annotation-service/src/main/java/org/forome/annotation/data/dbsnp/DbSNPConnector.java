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

package org.forome.annotation.data.dbsnp;

import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vcf.VariantVCF;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class DbSNPConnector {

	public List<String> getIds(AnfisaExecuteContext context, Variant variant) {
		String vcfVariantId = getVcfVariantId(variant);
		if (vcfVariantId != null) {
			return Collections.singletonList(vcfVariantId);
		}

		JSONArray jRecords = (JSONArray) context.sourceAStorageHttp.data.get("dbSNP");
		if (jRecords == null) {
			return Collections.emptyList();
		}

		List<String> ids = jRecords.stream()
				.map(o -> (JSONObject) o)
				.filter(item -> item.getAsString("REF").equals(variant.getRef()) && item.getAsString("ALT").equals(variant.getStrAlt()))
				.map(item -> item.getAsString("rs_id"))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		return ids;
	}

	private String getVcfVariantId(Variant variant) {
		if (!(variant instanceof VariantVCF)) {
			return null;
		}

		VariantVCF variantVCF = (VariantVCF) variant;
		VariantContext variantContext = variantVCF.maVariantVCF.variantContext;
		String id = variantContext.getID();

		if (id == null) return null;

		String tid = id.trim();
		if (".".equals(tid)) return null;
		if (tid.startsWith("chr")) return null; //В однои из vcf фалов встретился формат: chr1:565130_G/A

		return tid;
	}

}
