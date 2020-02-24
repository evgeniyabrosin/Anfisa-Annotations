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
import org.forome.annotation.service.database.batchrecord.compression.Compression;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.batch.BatchRecordConservation;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Упаковка идет последовательным укладом чисел
 * Для уменьшения объема, воспользуемся особенностью, т.к.
 * - нам интересно максимум 3 знака после запятой
 * - число > -31
 * - число < 31
 * Short.MIN_VALUE(-32768) - зарезервировано под null
 * то, сохраняемое число умножаем на 1000 и сохраняем как short которое размером в 2 байта
 */
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
		conservations[getIndex(position)] = conservation;
	}

	public Conservation getConservation(Position position) {
		return conservations[getIndex(position)];
	}

	private int getIndex(Position position) {
		return position.value - interval.start;
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
		List<Object[]> values = new ArrayList<>();
		for (int i = 0; i < conservations.length; i++) {
			Conservation conservation = conservations[i];
			if (conservation == null) {
				values.add(new Object[]{ null, null });
			} else {
				values.add(new Object[]{
						convertToShort(conservation.gerpRS),
						convertToShort(conservation.gerpN)
				});
			}
		}
		return new Compression(BatchRecordConservation.DATABASE_ORDER_TYPES, BatchRecord.DEFAULT_SIZE)
				.pack(values);
	}


	private static Short convertToShort(Float value) {
		if (value == null) {
			return null;
		} else {
			if (value > 31 || value < -32) {
				throw new IllegalStateException("value: " + value);
			}
			return (short) (value * 1000);
		}
	}
}
