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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class CompressionSelectiveWithDictionary extends AbstractCompression {

	@Override
	public byte[] pack(Class[] types, List<Object[]> items) throws NotSupportCompression {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		//Собираем словарь
		List<Object> dictionary = CompressionOrderWithDictionary.getDictionary(items);

		//Записываем размер словаря(без знаковый)
		os.write(ByteBits.convertFromUnsigned(dictionary.size()));

		//Последовательно записываем словарь
		for (Object iDictionary : dictionary) {
			try {
				os.write(pack(types[0], iDictionary));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		//Собираем не пустые записи
		LinkedHashMap<Integer, Object[]> notEmptyItems = CompressionSelective.getNotEmptyItems(types, items);

		//Записываем кол-во записей
		os.write(ByteBits.convertFromUnsigned(notEmptyItems.size()));

		//Записываем записи
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

				int dictionaryIndex = Integer.MIN_VALUE;
				for (int di = 0; di < dictionary.size(); di++) {
					if (Objects.equals(dictionary.get(di), value)) {
						dictionaryIndex = di;
						break;
					}
				}
				if (dictionaryIndex == Integer.MIN_VALUE) {
					//Баг, почему-то не нашлось значение в словаре
					throw new IllegalStateException();
				}
				os.write(ByteBits.convertFromUnsigned(dictionaryIndex));
			}
		}
		return os.toByteArray();
	}

	@Override
	public int unpackSize(Class[] types, int sizeInterval, byte[] bytes, int offsetBytes) {
		int size = 0;

		size++;//Размер карты

		int sizeMap = ByteBits.convertByUnsigned(bytes[offsetBytes]);
		size += CompressionOrderWithDictionary.getByteSize(types[0]) * sizeMap;//Сама карта

		int sizeRecord = ByteBits.convertByUnsigned(bytes[offsetBytes + size]);
		size++;//Кол-во записей
		size += (1 + types.length) * sizeRecord;//Сами записи

		return size;
	}

	@Override
	public Object[] unpackValues(Class[] types, byte[] bytes, int offsetBytes, int index) {
		int sizeMap = ByteBits.convertByUnsigned(bytes[offsetBytes]);

		int offsetWithMap = offsetBytes
				+ 1 //Размер карты
				+ CompressionOrderWithDictionary.getByteSize(types[0]) * sizeMap; //Сама карта

		int sizeRecord = ByteBits.convertByUnsigned(bytes[offsetWithMap]);

		for (int i = 0; i < sizeRecord; i++) {
			int offset = offsetWithMap //Начальное смещение
					+ 1 //Смещание на кол-во записей
					+ (1 + 1 * types.length) * i;

			int recortIndex = ByteBits.convertByUnsigned(bytes[offset]);

			if (recortIndex == index) {
				offset++; //Смещаем offset, т.к. в начале у нас содержится индекс записи
				Object[] value = new Object[types.length];
				for (int k = 0; k < types.length; k++) {
					Class type = types[k];

					int dictionaryIndex = ByteBits.convertByUnsigned(bytes[offset]);
					value[k] = unpackValue(type, bytes, offsetBytes + 1 + dictionaryIndex * CompressionOrderWithDictionary.getByteSize(type)).value;
					offset += 1;
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
