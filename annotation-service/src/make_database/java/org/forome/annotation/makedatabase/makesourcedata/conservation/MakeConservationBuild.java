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

package org.forome.annotation.makedatabase.makesourcedata.conservation;

import org.forome.annotation.service.database.struct.batch.BatchRecordConservation;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.utils.bits.IntegerBits;
import org.forome.annotation.utils.bits.ShortBits;

import java.nio.ByteBuffer;

/**
 * Упаковка идет последовательным укладом чисел
 * Для уменьшения объема, воспользуемся особенностью, т.к.
 * - нам интересно максимум 3 знака после запятой
 * - число > -31
 * - число < 31
 * Short.MIN_VALUE(-32768) - зарезервировано под null
 * то, сохраняемое число умножаем на 1000 и сохраняем как short которое размером в 2 байта
 */
public class MakeConservationBuild {

	public static class Data {

		public Float priPhCons = null;
		public Float mamPhCons = null;
		public Float verPhCons = null;
		public Float priPhyloP = null;
		public Float mamPhyloP = null;
		public Float verPhyloP = null;
		public Float gerpRS = null;
		public Float gerpRSpval = null;
		public Float gerpN = null;
		public Float gerpS = null;

		public Data() {
		}

		private boolean isEmpty() {
			if (priPhCons != null && Math.abs(priPhCons) > 0.00000001d) return false;
			if (mamPhCons != null && Math.abs(mamPhCons) > 0.00000001d) return false;
			if (verPhCons != null && Math.abs(verPhCons) > 0.00000001d) return false;
			if (priPhyloP != null && Math.abs(priPhyloP) > 0.00000001d) return false;
			if (mamPhyloP != null && Math.abs(mamPhyloP) > 0.00000001d) return false;
			if (verPhyloP != null && Math.abs(verPhyloP) > 0.00000001d) return false;
			if (gerpRS != null && Math.abs(gerpRS) > 0.00000001d) return false;
			if (gerpRSpval != null && Math.abs(gerpRSpval) > 0.00000001d) return false;
			if (gerpN != null && Math.abs(gerpN) > 0.00000001d) return false;
			if (gerpS != null && Math.abs(gerpS) > 0.00000001d) return false;
			return true;
		}
	}

	private final MakeConservationBuild.Data[] values;

	public MakeConservationBuild(Interval interval, MakeConservationBuild.Data[] values) {
		this.values = values;
		if (values.length != interval.end - interval.start + 1) {
			throw new IllegalArgumentException();
		}
	}

	public byte[] build() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(
				BatchRecordConservation.BYTE_SIZE_RECORD * values.length
		);
		for (int i = 0; i < values.length; i++) {
			Data item = values[i];
			if (item == null) {
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packInteger(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
				byteBuffer.put(packShort(null));
			} else {
				byteBuffer.put(packShort(item.priPhCons));
				byteBuffer.put(packShort(item.mamPhCons));
				byteBuffer.put(packShort(item.verPhCons));
				byteBuffer.put(packShort(item.priPhyloP));
				byteBuffer.put(packShort(item.mamPhyloP));
				byteBuffer.put(packShort(item.verPhyloP));
				byteBuffer.put(packInteger(item.gerpRS));
				byteBuffer.put(packInteger(item.gerpRSpval));
				byteBuffer.put(packShort(item.gerpN));
				byteBuffer.put(packInteger(item.gerpS));
			}
		}
		return byteBuffer.array();
	}

	private byte[] packShort(Float value) {
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

	private byte[] packInteger(Float value) {
		if (value == null) {
			return IntegerBits.toByteArray(BatchRecordConservation.INT_NULL_VALUE);
		} else {
			if (value > 2147482 || value < -2147482) {
				throw new IllegalStateException("value: " + value);
			}
			int iValue = (int) (value * 1000);
			return IntegerBits.toByteArray(iValue);
		}
	}

	public boolean isEmpty() {
		for (Data gerpData : values) {
			if (gerpData == null) continue;
			if (gerpData.isEmpty()) continue;
			return false;
		}
		return true;
	}
}
