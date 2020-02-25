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

package org.forome.annotation.service.database.struct.batch;

import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

public class BatchRecord {

	public static final int DEFAULT_SIZE = 200;

	public final Interval interval;
	protected final byte[] bytes;

	public final BatchRecordConservation batchRecordConservation;

	public BatchRecord(Interval interval, byte[] bytes) {
		this.interval = interval;
		this.bytes = bytes;

		int offset = 0;
		this.batchRecordConservation = new BatchRecordConservation(interval, bytes, offset);
		offset += batchRecordConservation.getLengthBytes();

	}

	public Record getRecord(Position position) {
		if (!interval.chromosome.equals(position.chromosome)) {
			throw new IllegalArgumentException();
		}
		if (position.value < interval.getMin()) {
			throw new IllegalArgumentException();
		}
		if (position.value > interval.getMax()) {
			throw new IllegalArgumentException();
		}

		return new Record(this, position);
	}

}
