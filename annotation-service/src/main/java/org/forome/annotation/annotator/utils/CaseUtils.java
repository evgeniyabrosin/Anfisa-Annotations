package org.forome.annotation.annotator.utils;

import org.forome.annotation.struct.Sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CaseUtils {

    public static SortedMap<String, Sample> parseFamFile(InputStream isFam, InputStream isFamSampleName) throws IOException {
        SortedMap<String, Sample> samples = new TreeMap<>();

        Map<String, String> sampleNameMap = new HashMap<>();
        if (isFamSampleName != null) {
            try (BufferedReader isBFamSampleName = new BufferedReader(new InputStreamReader(isFamSampleName))) {
                String line;
                while ((line = isBFamSampleName.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) continue;

                    String[] values = Arrays.stream(line.split("\t")).filter(s -> !s.trim().isEmpty()).toArray(String[]::new);
                    if (values.length != 4) {
                        throw new RuntimeException("Not support FamSampleName format");
                    }
                    String id = values[3];
                    String name = values[0];
                    sampleNameMap.put(id, name);
                }
            }
        }

        try (BufferedReader isBFam = new BufferedReader(new InputStreamReader(isFam))) {
            String line;
            while ((line = isBFam.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] sl = line.split("\t");

                String id = sl[1];
                String name = sampleNameMap.getOrDefault(id, id);
                String family = sl[0];
                String father = sl[2];
                String mother = sl[3];
                int sex = Integer.parseInt(sl[4]);
                boolean affected = Integer.parseInt(sl[5]) == 2;

                Sample sample = new Sample(id, name, family, father, mother, sex, affected);
                samples.put(id, sample);
            }
        }

        return samples;
    }

}
