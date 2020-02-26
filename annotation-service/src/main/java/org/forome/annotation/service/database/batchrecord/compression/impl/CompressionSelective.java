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

package org.forome.annotation.service.database.batchrecord.compression.impl;

import org.forome.annotation.service.database.batchrecord.compression.AbstractCompression;
import org.forome.annotation.service.database.batchrecord.compression.exception.NotSupportCompression;
import org.forome.annotation.utils.bits.ByteBits;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class CompressionSelective extends AbstractCompression {

	@Override
	public byte[] pack(Class[] types, List<Object[]> items) throws NotSupportCompression {
		//Собираем не пустые записи
		LinkedHashMap<Integer, Object[]> notEmptyItems = getNotEmptyItems(types, items);

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		//Записываем кол-во записей
		os.write(ByteBits.convertFromUnsigned(notEmptyItems.size()));

		for (int index : notEmptyItems.keySet()) {
			Object[] values = notEmptyItems.get(index);

			//Записываем индекс записи
			os.write(ByteBits.convertFromUnsigned(index));

			//Записываем значения
			for (int i = 0; i < values.length; i++) {
				Object value = values[i];
				if (value != null && value.getClass() != types[i]) {
					throw new IllegalStateException();
				}
				try {
					os.write(pack(types[i], value));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return os.toByteArray();
	}

	protected static LinkedHashMap<Integer, Object[]> getNotEmptyItems(Class[] types, List<Object[]> items) throws NotSupportCompression {
		LinkedHashMap<Integer, Object[]> notEmptyItems = new LinkedHashMap<>();
		for (int index = 0; index < items.size(); index++) {
			Object[] values = items.get(index);
			if (values.length != types.length) {
				throw new IllegalStateException();
			}
			if (Arrays.stream(values).filter(value -> (value != null)).count() == 0) {
				//Все записи оказались null, не будем записывать в базу
				continue;
			}

			if (index > ByteBits.MAX_UNSIGNED_VALUE) {
				//Не хватает разрядности для индекса (в 1 байт) - это сжатие не подходит
				throw new NotSupportCompression();
			}

			notEmptyItems.put(index, values);
		}
		return notEmptyItems;
	}

	@Override
	public int unpackSize(Class[] types, int sizeInterval, byte[] bytes, int offsetBytes) {
		int sizeRecord = ByteBits.convertByUnsigned(bytes[offsetBytes]);
		return 1 //1 байт на кол-во записей
				+ (1 //1 байт на индекс записи
				+ CompressionOrder.sizeRecord(types)//Размер записи
		)
				* sizeRecord;//Кол-во записей
	}

	@Override
	public Object[] unpackValues(Class[] types, byte[] bytes, int offsetBytes, int index) {
		int sizeRecord = ByteBits.convertByUnsigned(bytes[offsetBytes]);

		for (int i = 0; i < sizeRecord; i++) {
			int offset = offsetBytes //Начальное смещение
					+ 1 //Смещание на кол-во записей
					+ (1 + CompressionOrder.sizeRecord(types)) * i;

			int recortIndex = ByteBits.convertByUnsigned(bytes[offset]);

			if (recortIndex == index) {
				offset++; //Смещаем offset, т.к. в начале у нас содержится индекс записи
				Object[] value = new Object[types.length];
				for (int k = 0; k < types.length; k++) {
					Class type = types[k];

					UnpackObject unpackObject = unpackValue(type, bytes, offset);
					value[k] = unpackObject.value;
					offset += unpackObject.byteSize;
				}
				return value;
			} else if (recortIndex > index) {
				break;//Дальше искать не надо, т.к. там точно необходимого индекса уже не будет
			}
		}

		//Записей найденно не было
		return new Object[types.length];
	}
}
