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

package org.forome.annotation.makedatabase.make.batchrecord;

import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.struct.Interval;

public class WriteBatchRecord {

	public final Interval interval;

	private final WriteBatchRecordConservation writeBatchRecordConservation;

	public WriteBatchRecord(Interval interval) {
		this.interval = interval;

		this.writeBatchRecordConservation = new WriteBatchRecordConservation(interval);
	}

	public WriteBatchRecord(BatchRecord batchRecord) {
		this.interval = batchRecord.interval;

		this.writeBatchRecordConservation = new WriteBatchRecordConservation(
				batchRecord.batchRecordConservation
		);
	}

	public WriteBatchRecordConservation getBatchRecordConservation() {
		return writeBatchRecordConservation;
	}

	public boolean isEmpty() {
		return writeBatchRecordConservation.isEmpty();
	}

	public byte[] build() {
		return writeBatchRecordConservation.build();
	}
}
