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

package org.forome.annotation.processing.struct;

import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;

public class GContext {

	public final Source source;

	public final MCase mCase;
	public final Variant variant;
	public final ExecuteContext executeContext;

	//TODO Ulitin V. Удалить
	public final AnfisaConnector anfisaConnector;
	//TODO Ulitin V. Удалить
	public final AnfisaExecuteContext context;

	public GContext(
			Source source,
			MCase mCase, Variant variant,
			AnfisaConnector anfisaConnector, AnfisaExecuteContext context
	) {
		this.source = source;

		this.mCase = mCase;
		this.variant = variant;
		this.executeContext = new ExecuteContext(this);

		this.anfisaConnector = anfisaConnector;
		this.context = context;
	}


}
