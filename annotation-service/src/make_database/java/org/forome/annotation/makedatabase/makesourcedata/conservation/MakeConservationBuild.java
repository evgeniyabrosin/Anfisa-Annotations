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

import org.forome.annotation.struct.Interval;
import org.forome.annotation.utils.bits.ShortBits;

import java.nio.ByteBuffer;

/**
 * Упаковка идет последовательным укладом чисел gerpN и gerpRS
 * Для уменьшения объема, воспользуемся особенностью, т.к.
 * - нам интересно максимум 3 знака после запятой
 * - число > -31
 * - число < 31
 * то, сохраняемое число умножаем на 1000 и сохраняем как short которое размером в 2 байта
 */
public class MakeConservationBuild {

	public static class GerpData {

		public final Float gerpN;
		public final Float gerpRS;

		public GerpData(Float gerpN, Float gerpRS) {
			this.gerpN = gerpN;
			this.gerpRS = gerpRS;
		}

		private boolean isEmpty() {
			if (Math.abs(gerpN) > 0.00000001d) return false;
			if (Math.abs(gerpRS) > 0.00000001d) return false;
			return true;
		}
	}

	private final Interval interval;
	private final MakeConservationBuild.GerpData[] values;

	public MakeConservationBuild(Interval interval, MakeConservationBuild.GerpData[] values) {
		this.interval = interval;
		this.values = values;
	}

	public byte[] build() {
		ByteBuffer byteBuffer = ByteBuffer.allocate((2 + 2) * values.length);
		for (int i = 0; i < values.length; i++) {
			GerpData item = values[i];

			short sGerpN;
			short sGerpRS;
			if (item == null) {
				sGerpN = 0;
				sGerpRS = 0;
			} else {
				if (item.gerpN > 31 || item.gerpN < -31) {
					throw new IllegalStateException();
				}
				if (item.gerpRS > 31 || item.gerpRS < -31) {
					throw new IllegalStateException();
				}
				sGerpN = (short) (item.gerpN * 1000);
				sGerpRS = (short) (item.gerpRS * 1000);
			}

			byteBuffer.put(ShortBits.toByteArray(sGerpN));
			byteBuffer.put(ShortBits.toByteArray(sGerpRS));
		}
		return byteBuffer.array();
	}

	public boolean isEmpty() {
		for (GerpData gerpData : values) {
			if (gerpData == null) continue;
			if (gerpData.isEmpty()) continue;
			return false;
		}
		return true;
	}
}
