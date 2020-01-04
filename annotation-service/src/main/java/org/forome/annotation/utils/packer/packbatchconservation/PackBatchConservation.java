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

package org.forome.annotation.utils.packer.packbatchconservation;


import org.forome.annotation.connector.conservation.struct.BatchConservation;
import org.forome.annotation.connector.conservation.struct.ConservationItem;
import org.forome.annotation.struct.Interval;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;

public class PackBatchConservation {

	/**
	 * Упаковка идет последовательным укладом чисел gerpN и gerpRS
	 * Для уменьшения объема, воспользуемся особенностью, т.к.
	 * - нам интересно максимум 3 знака после запятой
	 * - число > -31
	 * - число < 31
	 * то, сохраняемое число умножаем на 1000 и сохраняем как short которое размером в 2 байта
	 *
	 * @param value
	 * @return
	 */
	public static byte[] toByteArray(BatchConservation value) {
		//Собраем значения которые нам необходимо упаковать
		short[] packValues = new short[value.items.length * 2];
		for (int i = 0; i < value.items.length; i++) {
			ConservationItem item = value.items[i];

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

			packValues[i*2]=sGerpN;
			packValues[i*2+1]=sGerpRS;
		}

		LinkedHashSet<Short> dictionary = new LinkedHashSet<>();
		for (short s: packValues) {
			dictionary.add(s);
		}









		int size = value.interval.end - value.interval.start + 1;

		byte[] bytes = new byte[(4 + 4) * size];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

		//Нужно посчитать статистику сколько нулей
		//select count(*) from conservation.GERP where GerpN = 0;
		//select count(*) from conservation.GERP where GerpRS = 0;
		//из 3 091 684 192 записей

		for (int i = 0; i < size; i++) {
			int position = value.interval.start + i;
			ConservationItem item = value.getConservation(position);

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

			byteBuffer.putShort(sGerpN);
			byteBuffer.putShort(sGerpRS);
		}

		return bytes;
	}

	public static BatchConservation fromByteArray(Interval interval, byte[] bytes) {
		ConservationItem[] items = new ConservationItem[bytes.length / 8];

		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		for (int i = 0; i < items.length; i++) {
			float gerpN = (float) byteBuffer.getShort() / 1000.0f;
			float gerpRS = (float) byteBuffer.getShort() / 1000.0f;
			items[i] = new ConservationItem(gerpN, gerpRS);
		}

		return new BatchConservation(interval, items);
	}
}
