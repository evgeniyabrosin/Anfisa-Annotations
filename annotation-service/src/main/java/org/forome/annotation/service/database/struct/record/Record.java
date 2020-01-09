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

package org.forome.annotation.service.database.struct.record;

import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.struct.Position;

public class Record {

	private final BatchRecord batchRecord;
	public final Position position;

	public Record(BatchRecord batchRecord, Position position) {
		this.batchRecord = batchRecord;
		this.position = position;
	}

	public RecordConservation getRecordConservation() {
		return new RecordConservation(this, batchRecord.batchRecordConservation);
	}
}
