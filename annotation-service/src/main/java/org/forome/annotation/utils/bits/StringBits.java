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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringBits {

	public static final Charset CHARSET = StandardCharsets.UTF_8;

	public static byte[] NULL_STRING = new byte[] { (byte) 0xff };

	public static String fromByteArray(byte[] bytes) {
		return fromByteArray(bytes, 0, bytes.length);
	}

	public static String fromByteArray(byte[] bytes, int offset, int length) {
		if (length == 1 && bytes[offset] == NULL_STRING[0]) {
			return null;
		} else {
			return new String(bytes, offset, length, CHARSET);
		}
	}

	public static byte[] toByteArray(String value) {
		return value.getBytes(CHARSET);
	}

}
