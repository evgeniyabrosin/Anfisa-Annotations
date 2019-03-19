package org.forome.annotation.annotator.utils;

import org.forome.annotation.struct.Sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.TreeMap;

public class CaseUtils {

    public static SortedMap<String, Sample> parseFamFile(InputStream isFam) throws IOException {
        SortedMap<String, Sample> samples = new TreeMap<>();

        try (BufferedReader isBFam = new BufferedReader(new InputStreamReader(isFam))) {
            String line;
            while ((line = isBFam.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] sl = line.split("\t");

                String id = sl[1];
                String name = id;
                String family = sl[0];
                String father = sl[2];
                String mother = sl[3];
                int sex = Integer.parseInt( sl[4]);
                boolean affected = Integer.parseInt( sl[5]) == 2;

                Sample sample = new Sample(id, name, family, father, mother, sex, affected);
                samples.put(id, sample);
            }
        }

        return samples;
    }

}
