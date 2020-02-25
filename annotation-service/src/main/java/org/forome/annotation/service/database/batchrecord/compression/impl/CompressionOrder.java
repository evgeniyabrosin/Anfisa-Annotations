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
import org.forome.annotation.utils.bits.ShortBits;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Простое последовательное укладывание значений
 */
public class CompressionOrder extends AbstractCompression {

	public CompressionOrder() {
	}

	public byte[] pack(Class[] types, List<Object[]> items) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (Object[] values : items) {
			if (values.length != types.length) {
				throw new IllegalStateException();
			}
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

	@Override
	public int unpackSize(Class[] types, int sizeInterval, byte[] bytes, int offsetBytes) {
		return sizeRecord(types) * sizeInterval;
	}

	public static int sizeRecord(Class[] types) {
		int size = 0;
		for (Class type : types) {
			if (type == Short.class || type == short.class) {
				size += ShortBits.BYTE_SIZE;
			} else {
				throw new RuntimeException("Not support type: " + type);
			}
		}
		return size;
	}

	public Object[] unpackValues(Class[] types, byte[] bytes, int offsetBytes, int index) {
		int offset = offsetBytes + sizeRecord(types) * index;

		Object[] value = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			Class type = types[i];

			UnpackObject unpackObject = unpackValue(type, bytes, offset);
			value[i] = unpackObject.value;
			offset += unpackObject.byteSize;
		}
		return value;
	}


}
