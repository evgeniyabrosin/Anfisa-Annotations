package org.forome.annotation.annotator.utils;

import java.util.Collection;

public class SampleUtils {

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
