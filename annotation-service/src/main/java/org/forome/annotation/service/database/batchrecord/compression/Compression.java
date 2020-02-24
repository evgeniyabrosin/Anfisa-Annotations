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

package org.forome.annotation.service.database.batchrecord.compression;

import org.forome.annotation.service.database.batchrecord.compression.exception.NotSupportCompression;

import java.util.List;

public class Compression {

	private final Class[] types;
	private final int sizeInterval;

	public Compression(Class[] types, int sizeInterval) {
		this.types = types;
		this.sizeInterval = sizeInterval;
	}

	public byte[] pack(List<Object[]> items) {
		if (items.size() != sizeInterval) {
			throw new IllegalArgumentException();
		}

		//Последовательно упаковываем различными способами и выбираем сжатие с минимальным размером
		TypeCompression optimalTypeCompression = null;
		byte[] optimalPack = null;
		for (TypeCompression type : TypeCompression.values()) {
			byte[] pack;
			try {
				pack = type.compression.pack(types, items);
			} catch (NotSupportCompression ignore) {
				//Указанный алгоритм сжатия не поддерживает эти данные
				continue;
			}
			if (optimalPack == null || optimalPack.length > pack.length) {
				optimalTypeCompression = type;
				optimalPack = pack;
			}
		}

		byte[] result = new byte[optimalPack.length + 1];
		result[0] = optimalTypeCompression.value;
		System.arraycopy(optimalPack, 0, result, 1, optimalPack.length);
		return result;
	}

	public int unpackSize(byte[] bytes, int offsetBytes) {
		TypeCompression typeCompression = TypeCompression.get(bytes[offsetBytes]);
		return typeCompression.compression.unpackSize(types, sizeInterval, bytes, offsetBytes + 1) + 1;
	}

	public Object[] unpackValues(byte[] bytes, int offsetBytes, int index) {
		TypeCompression typeCompression = TypeCompression.get(bytes[offsetBytes]);
		return typeCompression.compression.unpackValues(types, bytes, offsetBytes + 1, index);
	}
}
