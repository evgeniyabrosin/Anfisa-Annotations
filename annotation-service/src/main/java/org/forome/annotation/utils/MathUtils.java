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

package org.forome.annotation.utils;

public class MathUtils {

	public static Double toDouble(Object value) {
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return Double.parseDouble((String) value);
		} else {
			throw new RuntimeException("Not support type");
		}
	}

	public static double toPrimitiveDouble(Object value) {
		Double p = toDouble(value);
		if (p == null) {
			return 0;
		} else {
			return p;
		}
	}

	public static Integer toInteger(Object value) {
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else if (value instanceof String) {
			return Integer.parseInt((String) value);
		} else {
			throw new RuntimeException("Not support type");
		}
	}

	public static int toPrimitiveInteger(Object value) {
		Integer p = toInteger(value);
		if (p == null) {
			return 0;
		} else {
			return p;
		}
	}

	public static Float toFloat(Object value) {
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).floatValue();
		} else if (value instanceof String) {
			return Float.parseFloat((String) value);
		} else {
			throw new RuntimeException("Not support type");
		}
	}

	public static float toPrimitiveFloat(Object value) {
		Float p = toFloat(value);
		if (p == null) {
			return 0;
		} else {
			return p;
		}
	}
}
