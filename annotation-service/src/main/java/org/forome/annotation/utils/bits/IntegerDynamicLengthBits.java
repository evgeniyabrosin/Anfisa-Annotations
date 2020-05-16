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

package org.forome.annotation.utils.bits;

import java.util.ArrayList;
import java.util.List;

public class IntegerDynamicLengthBits {

	public static class Value {

		public final int value;
		public final int length;

		public Value(int value, int length) {
			this.value = value;
			this.length = length;
		}
	}

	public static byte[] toByteArray(int value) {
		if (value < 0) throw new IllegalArgumentException();

		if (value <= Byte.MAX_VALUE) {
			return new byte[]{ (byte) value };
		} else {
			List<Integer> bytes = new ArrayList<>();
			while (value > 0) {
				int b = value % (Byte.MAX_VALUE + 1);
				bytes.add(b);
				value = value / (Byte.MAX_VALUE + 1);
			}

			byte[] result = new byte[bytes.size()];
			for (int i = 1; i < bytes.size(); i++) {
				result[i - 1] = (byte) (-(bytes.get(i) + 1));
			}
			result[bytes.size() - 1] = bytes.get(0).byteValue();

			return result;
		}
	}

	public static Value fromByteArray(byte[] bytes, int offset) {
		int index = offset;

		int value = 0;
		int length = 0;

		while (true) {
			byte b = bytes[index++];
			length++;
			if (b >= 0) {
				value += b;
				break;
			} else {
				value += -(b + 1) * (Math.pow(Byte.MAX_VALUE + 1, length));
			}
		}

		return new Value(value, length);
	}
}
