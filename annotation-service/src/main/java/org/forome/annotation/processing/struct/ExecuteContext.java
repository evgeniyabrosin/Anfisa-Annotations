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

package org.forome.annotation.processing.struct;

import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.struct.variant.Variant;
import org.forome.astorage.core.data.Conservation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExecuteContext {

	private final String CACHE_CONSERVATION = "conservation";

	private final GContext gContext;
	private final Map<String, Object> cache;

	public ExecuteContext(GContext gContext) {
		this.gContext = gContext;
		this.cache = new HashMap<>();
	}

	public Conservation getConservation() {
		Optional<Conservation> oConservation = (Optional<Conservation>) cache.computeIfAbsent(CACHE_CONSERVATION, s -> {
			Variant variant = gContext.variant;
			String ref = variant.getRef();
			String alt = variant.getStrAlt();
			Conservation conservation = new ConservationData(gContext.source)
					.getConservation(variant.getInterval(), ref, alt);
			return Optional.ofNullable(conservation);
		});
		return oConservation.orElse(null);
	}
}
