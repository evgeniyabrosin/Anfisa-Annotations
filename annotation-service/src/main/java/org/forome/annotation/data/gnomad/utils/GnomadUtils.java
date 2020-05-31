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

package org.forome.annotation.data.gnomad.utils;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GnomadUtils {

	public static String diff(String s1, String s2) {
		if (s1.equals(s2)) {
			return "";
		} else if (s2.contains(s1)) {
			int idx = s2.indexOf(s1);
			return s2.substring(0, idx) + s2.substring(idx + s1.length());
		} else if (s1.length() < s2.length()) {
			List<Character> x = new ArrayList<>();
			List<Character> y = new ArrayList<>();
			for (int i = 0; i < s2.length(); i++) {
				int j = x.size();
				if (j < s1.length() && s1.charAt(j) == s2.charAt(i)) {
					x.add(s2.charAt(i));
				} else {
					y.add(s2.charAt(i));
				}
			}
			if (!y.isEmpty()) {
				return y.stream().map(e -> e.toString()).collect(Collectors.joining());
			}
			return null;
		} else if (s2.length() < s1.length()) {
			return "-" + diff(s2, s1);
		} else {
			return null;
		}
	}

	public static boolean diff3(String s1, String s2, String d) {
		if (Strings.isNullOrEmpty(d)) {
			return s1.equals(s2);
		}
		if (d.charAt(0) == '-') {
			return diff3(s2, s1, d.substring(1));
		}
		if (s1.length() + d.length() != s2.length()) {
			return false;
		}
		for (int i = 0; i < s1.length(); i++) {
			String x = s1.substring(0, i);
			String y = s1.substring(i);
			if ((x + d + y).equals(s2)) {
				return true;
			}
		}
		return false;
	}
}
