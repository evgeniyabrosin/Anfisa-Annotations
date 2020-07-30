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

package org.forome.annotation.struct.mavariant;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

import java.util.stream.Collectors;

public class MAVariantVCF extends MAVariantVep {

	public final VariantContext variantContext;

	public MAVariantVCF(VariantContext variantContext) {
		this.variantContext = variantContext;
	}

	@Override
	public String toString() {
		String alts = variantContext.getAlternateAlleles().stream()
				.map(Allele::getBaseString).collect(Collectors.joining(","));

		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("MAVariantVCF{")
				.append(variantContext.getContig()).append(':')
				.append(variantContext.getStart()).append(' ')
				.append(variantContext.getReference().getBaseString()).append('>')
				.append(alts);
		sBuilder.append('}');
		return sBuilder.toString();
	}
}
