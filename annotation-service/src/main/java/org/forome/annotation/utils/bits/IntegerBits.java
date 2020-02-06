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

import com.google.common.primitives.Ints;

public class IntegerBits {

	public static int fromByteArray(byte[] bytes, int offset) {
		return Ints.fromBytes(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
	}

	public static int fromByteArray(byte[] bytes) {
		return fromByteArray(bytes, 0);
	}

	public static byte[] toByteArray(int value) {
		return Ints.toByteArray(value);
	}
}
