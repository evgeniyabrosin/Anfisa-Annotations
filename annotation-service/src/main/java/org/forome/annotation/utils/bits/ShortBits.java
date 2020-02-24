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

public class ShortBits {

	public static int BYTE_SIZE = 2;

	public static short fromByteArray(byte[] bytes, int offset) {
		return (short) ((bytes[offset] << 8) | (bytes[offset + 1] & 0xff));
	}

	public static short fromByteArray(byte[] bytes) {
		return fromByteArray(bytes, 0);
	}

	public static byte[] toByteArray(short value) {
		byte[] bytes = new byte[BYTE_SIZE];
		bytes[0] = (byte) (value >> 8);
		bytes[1] = (byte) (value);
		return bytes;
	}
}
