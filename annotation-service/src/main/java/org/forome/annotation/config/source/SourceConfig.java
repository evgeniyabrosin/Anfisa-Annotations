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

package org.forome.annotation.config.source;

import net.minidev.json.JSONObject;

public class SourceConfig {

	private final static String FIELD_INTERNAL = "internal";
	private final static String FIELD_EXTERNAL = "external";

	public final SourceInternalConfig sourceInternalConfig;
	public final SourceExternalConfig sourceExternalConfig;

	public SourceConfig(JSONObject parse) {
		if (parse.containsKey(FIELD_INTERNAL)) {
			sourceInternalConfig = new SourceInternalConfig((JSONObject) parse.get(FIELD_INTERNAL));
		} else {
			sourceInternalConfig = null;
		}

		if (parse.containsKey(FIELD_EXTERNAL)) {
			sourceExternalConfig = new SourceExternalConfig((JSONObject) parse.get(FIELD_EXTERNAL));
		} else {
			sourceExternalConfig = null;
		}

		if (sourceInternalConfig != null && sourceExternalConfig != null) {
			throw new RuntimeException("Conflict configuration");
		}
	}
}
