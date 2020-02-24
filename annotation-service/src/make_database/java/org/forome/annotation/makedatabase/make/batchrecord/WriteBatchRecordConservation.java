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

import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.service.database.struct.batch.BatchRecordConservation;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.utils.bits.ShortBits;

import java.nio.ByteBuffer;

public class WriteBatchRecordConservation {

	private final Interval interval;
	private final Conservation[] conservations;

	public WriteBatchRecordConservation(Interval interval) {
		this.interval = interval;
		this.conservations = new Conservation[interval.end - interval.start + 1];
	}

	public WriteBatchRecordConservation(BatchRecordConservation batchRecordConservation) {
		this(batchRecordConservation.interval);

		for (int i = 0; i < conservations.length; i++) {
			Position position = new Position(interval.chromosome, interval.start + i);
			set(position, batchRecordConservation.getConservation(position));
		}
	}

	public void set(Position position, Conservation conservation) {
		conservations[position.value - interval.start] = conservation;
	}

	public boolean isEmpty() {
		for (Conservation conservation : conservations) {
			if (conservation == null) continue;
			if (isEmpty(conservation)) continue;
			return false;
		}
		return true;
	}

	public static boolean isEmpty(Conservation conservation) {
		if (conservation.gerpRS != null && Math.abs(conservation.gerpRS) > 0.00000001d) return false;
		if (conservation.gerpN != null && Math.abs(conservation.gerpN) > 0.00000001d) return false;
		return true;
	}

	public byte[] build() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(
				BatchRecordConservation.BYTE_SIZE_RECORD * conservations.length
		);
		for (int i = 0; i < conservations.length; i++) {
			Conservation conservation = conservations[i];
			if (conservation == null) {
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
			} else {
				byteBuffer.put(packShort(conservation.gerpRS));
				byteBuffer.put(packShort(conservation.gerpN));
			}
		}
		return byteBuffer.array();
	}



	private static byte[] packShort(Float value) {
		if (value == null) {
			return ShortBits.toByteArray(BatchRecordConservation.SHORT_NULL_VALUE);
		} else {
			if (value > 31 || value < -32) {
				throw new IllegalStateException("value: " + value);
			}
			short sValue = (short) (value * 1000);
			return ShortBits.toByteArray(sValue);
		}
	}
}
