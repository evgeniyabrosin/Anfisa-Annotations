package org.forome.annotation.utils;

import com.google.common.base.Objects;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JSONEquals {

	private static Pattern PATTERN_LABEL = Pattern.compile(
			"^\\[(.*)\\](.*)$"
	);

	public static void equals(JSONObject value1, JSONObject value2) throws Exception {
		if (value1.size() != value2.size()) {
			Set<String> diff;
			if (value1.size() > value2.size()) {
				diff = new HashSet<String>(value1.keySet()) {{
					removeAll(value2.keySet());
				}};
			} else {
				diff = new HashSet<String>(value2.keySet()) {{
					removeAll(value1.keySet());
				}};
			}
			throw new Exception("Not equals size JSONObject: " + value1.size() + " and " + value2.size() + ", diff: " + diff);
		}
		for (Map.Entry<String, Object> entry : value1.entrySet()) {
			try {
				equals(entry.getKey(), entry.getValue(), value2.get(entry.getKey()));
			} catch (Exception e) {
				throw new Exception("Exception equals value, key: \"" + entry.getKey() + "\"", e);
			}
		}
	}

	public static void equals(JSONArray value1, JSONArray value2) throws Exception {
		if (value1.size() != value2.size()) {
			throw new Exception("Not equals size JSONArray: " + value1.size() + " and " + value2.size());
		}

		List<Object> sortValue1;
		List<Object> sortValue2;
		if (value1.get(0) instanceof Number) {
			sortValue1 = value1.stream().map(it -> ((Number)it).doubleValue()).sorted().collect(Collectors.toList());
			sortValue2 = value2.stream().map(it -> ((Number)it).doubleValue()).sorted().collect(Collectors.toList());
		} else {
			sortValue1 = value1.stream().sorted().collect(Collectors.toList());
			sortValue2 = value2.stream().sorted().collect(Collectors.toList());
		}
		for (int i = 0; i < sortValue1.size(); i++) {
			equals(null, sortValue1.get(i), sortValue2.get(i));
		}
	}

	private static void equals(String key, Object value1, Object value2) throws Exception {
		if (Objects.equal(value1, value2)) {
			return;
		} else if (value1 == null && value2 != null) {
			throw new Exception("Not equals: null and " + value2.toString());
		} else if (value1 != null && value2 == null) {
			throw new Exception("Not equals: " + value1.toString() + " and null");
		} else if (value1 instanceof Number && value2 instanceof Number) {
			if (Math.abs(((Number) value1).doubleValue() - ((Number) value2).doubleValue()) < 0.000001D) {
				return;
			}
		}

		if (value1.getClass() != value2.getClass()) {
			throw new Exception("Not equals type: " + value1.getClass() + " and " + value2.getClass());
		}

		if (value1 instanceof JSONObject) {
			equals((JSONObject) value1, (JSONObject) value2);
		} else if (value1 instanceof JSONArray) {
			equals((JSONArray) value1, (JSONArray) value2);
		} else if (value1 instanceof String) {
			if (!value1.equals(value2)) {
				//Исключение связанное с разницей вычисления(с плавающе	точкой)
				if ("pop_max".equals(key) && value1 != null && value2 != null) {
					String[] value1s = ((String) value1).split(" ");
					String[] value2s = ((String) value2).split(" ");
					if (Objects.equal(value1s[0], value2s[0]) &&
							Math.abs((Double.parseDouble(value1s[1]) - Double.parseDouble(value2s[1]))) < 0.00000001d &&
							Objects.equal(value1s[2], value2s[2])) {
						return;
					}
				} else if ("label".equals(key) && value1 != null && value2 != null) {
					Matcher matcher1 = PATTERN_LABEL.matcher((String) value1);
					matcher1.matches();
					Set<String> value1s = new HashSet<String>(Arrays.asList(matcher1.group(1).split(",")));

					Matcher matcher2 = PATTERN_LABEL.matcher((String) value2);
					matcher2.matches();
					Set<String> value2s = new HashSet<String>(Arrays.asList(matcher2.group(1).split(",")));

					value1s.removeAll(value2s);
					if (value1s.size()==0) {
						return;
					}
				}
				throw new Exception("Not equals: " + value1.toString() + " and " + value2.toString());
			}
		} else {
			throw new Exception("Not support type: " + value1.getClass());
		}
	}

}
