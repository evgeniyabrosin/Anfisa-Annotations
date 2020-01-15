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

package org.forome.annotation.data.anfisa.struct;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.variant.Variant;

public class AnfisaExecuteContext {

	public final AnfisaInput anfisaInput;

	public final Variant variant;
	public final JSONObject vepJson;

	public Double gnomadAfFam;

	public AnfisaExecuteContext(
			AnfisaInput anfisaInput,
			Variant variant,
			JSONObject vepJson
	) {
		this.anfisaInput = anfisaInput;

		this.variant = variant;
		this.vepJson = vepJson;
	}

}
