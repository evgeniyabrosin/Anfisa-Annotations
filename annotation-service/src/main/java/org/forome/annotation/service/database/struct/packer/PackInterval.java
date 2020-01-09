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

package org.forome.annotation.service.database.struct.packer;

import com.google.common.primitives.Ints;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;

public class PackInterval {

	public int size;

	public PackInterval() {
		this(BatchRecord.DEFAULT_SIZE);
	}

	public PackInterval(int size) {
		this.size = size;
	}

	public Interval fromByteArray(byte[] bytes) {
		if (bytes.length != 4) {
			throw new IllegalArgumentException();
		}

		int k = Ints.fromBytes((byte) 0, bytes[1], bytes[2], bytes[3]);

		Chromosome chromosome = PackChromosome.fromByte(bytes[0]);
		int start = k * size;
		int end = start + size - 1;

		return Interval.of(chromosome, start, end);
	}

	public byte[] toByteArray(Interval value) {
		if (value.end - value.start + 1 != size) {
			throw new IllegalArgumentException();
		}
		if (value.start % size != 0) {
			throw new IllegalArgumentException();
		}
		int k = value.start / size;

		byte[] bytes = Ints.toByteArray(k);
		if (bytes[0] != 0) {
			//Валидируем размер - первый байт должен быть свободным
			throw new IllegalArgumentException();
		}

		bytes[0] = PackChromosome.toByte(value.chromosome);

		return bytes;
	}

}
