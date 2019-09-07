package org.forome.annotation.annotator.utils;

import org.forome.annotation.struct.Sample;

import java.util.Collection;
import java.util.Map;

public class SampleUtils {

    public static String getProband(Map<String, Sample> samples) {
        if (samples == null) {
            return null;
        } else if (samples.size() == 1) {
            return samples.values().iterator().next().id;
        }
        for (Map.Entry<String, Sample> entry : samples.entrySet()) {
            if (isProband(entry.getValue().id)) {
                return entry.getValue().id;
            }
        }
        return null;
    }

    public static String getProband(Collection<String> samples) {
        if (samples == null) {
            return null;
        } else if (samples.size() == 1) {
            return samples.iterator().next();
        }
        for (String sample : samples) {
            if (isProband(sample)) {
                return sample;
            }
        }
        return null;
    }

    private static boolean isProband(String sample) {
        return sample.endsWith("a1");
    }
}
