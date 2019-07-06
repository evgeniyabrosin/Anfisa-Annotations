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

    private static Pattern PATTERN_MAX_ENT_SCAN = Pattern.compile(
            "^(.*)=(.*[^\\-])-(.*)$"
    );
    private static Pattern PATTERN_CANONICAL_ANNOTATION = Pattern.compile(
            "^(.*)\\[(.*)\\]$"
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
        if (isNumberValues(value1) && isNumberValues(value2)) {
            sortValue1 = value1.stream().map(it -> ((Number) it).doubleValue()).sorted().collect(Collectors.toList());
            sortValue2 = value2.stream().map(it -> {
                if (it instanceof String) {
                    return Double.parseDouble((String) it);
                } else {
                    return ((Number) it).doubleValue();
                }
            }).sorted().collect(Collectors.toList());
        } else if (value1.get(0) instanceof JSONObject) {
            sortValue1 = value1.stream().collect(Collectors.toList());
            sortValue2 = value2.stream().collect(Collectors.toList());
        } else if (value1.get(0) instanceof String || value2.get(0) instanceof String) {
            sortValue1 = value1.stream().map(o -> String.valueOf(o)).sorted().collect(Collectors.toList());
            sortValue2 = value2.stream().map(o -> String.valueOf(o)).sorted().collect(Collectors.toList());
        } else {
            sortValue1 = value1.stream().sorted().collect(Collectors.toList());
            sortValue2 = value2.stream().sorted().collect(Collectors.toList());
        }
        for (int i = 0; i < sortValue1.size(); i++) {
            equals(null, sortValue1.get(i), sortValue2.get(i));
        }
    }

    private static boolean isNumberValues(JSONArray value) {
        for (Object item : value) {
            if (!(item instanceof Number)) return false;
        }
        return true;
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
            } else {
                throw new Exception(String.format("Not equals: %s and %s", value1, value2));
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
                if ("pop_max".equals(key) && value1 != null && value2 != null) {//Исключение связанное с разницей вычисления(с плавающе	точкой)
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
                    if (value1s.size() == 0) {
                        return;
                    }
                } else if (
                        (
                                "total_exon_intron_worst".equals(key)
                                        || "variant_exon_intron_worst".equals(key)
                                        || "variant_exon_intron_canonical".equals(key)
                                        || "total_exon_intron_canonical".equals(key)
                        ) && value1 != null && value2 != null) {
                    List<String> value1s = Arrays.stream(((String) value1).split(",")).collect(Collectors.toList());
                    List<String> value2s = Arrays.stream(((String) value2).split(",")).collect(Collectors.toList());
                    if (value1s.size() == value2s.size()) {
                        for (String item : value1s) {
                            value2s.remove(item);
                        }
                        if (value2s.isEmpty()) {
                            return;
                        }
                    }
                } else if (PATTERN_MAX_ENT_SCAN.matcher((String) value1).matches() && PATTERN_MAX_ENT_SCAN.matcher((String) value2).matches()) {
                    Matcher matcher1 = PATTERN_MAX_ENT_SCAN.matcher((String) value1);
                    matcher1.matches();
                    double v11 = Double.parseDouble(matcher1.group(1));
                    double v12 = Double.parseDouble(matcher1.group(2));
                    double v13 = Double.parseDouble(matcher1.group(3));

                    Matcher matcher2 = PATTERN_MAX_ENT_SCAN.matcher((String) value2);
                    matcher2.matches();
                    double v21 = Double.parseDouble(matcher2.group(1));
                    double v22 = Double.parseDouble(matcher2.group(2));
                    double v23 = Double.parseDouble(matcher2.group(3));

                    if (Math.abs(v11 - v21) < 0.000001D &&
                            Math.abs(v12 - v22) < 0.000001D &&
                            Math.abs(v13 - v23) < 0.000001D
                    ) {
                        return;
                    }
                } else if ("canonical_annotation".equals(key)) {
                    Matcher matcher1 = PATTERN_CANONICAL_ANNOTATION.matcher((String) value1);
                    matcher1.matches();
                    String v1m = matcher1.group(1);
                    List<String> v1v = Arrays.stream(matcher1.group(2).split(",")).map(s -> s.trim()).collect(Collectors.toList());

                    Matcher matcher2 = PATTERN_CANONICAL_ANNOTATION.matcher((String) value2);
                    matcher2.matches();
                    String v2m = matcher2.group(1);
                    List<String> v2v = Arrays.stream(matcher2.group(2).split(",")).map(s -> s.trim()).collect(Collectors.toList());

                    if (v1m.equals(v2m) && v1v.size() == v2v.size()) {
                        Set<String> v1vs = new HashSet<>(v1v);
                        Set<String> v2vs = new HashSet<>(v2v);
                        if (v1vs.size() == v2vs.size()) {
                            v1vs.removeAll(v2vs);
                            if (v1vs.isEmpty()) return;
                        }
                    }
                }
                throw new Exception("Not equals: '" + value1.toString() + "' and '" + value2.toString() + "'");
            }
        } else {
            throw new Exception("Not support type1: " + value1.getClass() + ", type2: " + value2.getClass());
        }
    }

}
