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

package org.forome.annotation.service.source.internal.common;

import net.minidev.json.JSONArray;
import org.forome.astorage.pastorage.PAStorage;
import org.forome.astorage.pastorage.schema.SchemaCommon;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;

public class CommonSourcePortPython {

	private final PAStorage paStorage;

	public CommonSourcePortPython(PAStorage paStorage) {
		this.paStorage = paStorage;
	}

	public JSONArray get(String schemaName, Assembly assembly, Interval interval) {
		SchemaCommon schemaCommon = (SchemaCommon) paStorage.getSchema(schemaName);

		JSONArray jRecords = schemaCommon.blocker.getRecord(
				assembly,
				new Position(interval.chromosome, interval.start)
		);
		return jRecords;
	}
}
