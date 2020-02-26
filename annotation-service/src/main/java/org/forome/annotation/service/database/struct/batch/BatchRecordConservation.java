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

import com.google.common.annotations.VisibleForTesting;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.service.database.batchrecord.compression.Compression;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

public class BatchRecordConservation {

	public static Class[] DATABASE_ORDER_TYPES = { Short.class, Short.class };

	public final Interval interval;
	private final byte[] bytes;
	private final int offsetBytes;

	private final Compression compression;

	@VisibleForTesting
	public BatchRecordConservation(Interval interval, byte[] bytes, int offsetBytes) {
		this.interval = interval;
		this.bytes = bytes;
		this.offsetBytes = offsetBytes;

		compression = new Compression(DATABASE_ORDER_TYPES, interval.end - interval.start);
	}

	public Conservation getConservation(Position position) {
		int index = position.value - interval.start;
		Object[] values = compression.unpackValues(bytes, offsetBytes, index);

		return new Conservation(
				convertByShort((Short) values[0]),
				convertByShort((Short) values[1])
		);
	}

	private static Float convertByShort(Short value) {
		if (value == null) {
			return null;
		} else {
			return (float) value / 1000.0f;
		}
	}

	protected int getLengthBytes() {
		return compression.unpackSize(bytes, offsetBytes);
	}
}
