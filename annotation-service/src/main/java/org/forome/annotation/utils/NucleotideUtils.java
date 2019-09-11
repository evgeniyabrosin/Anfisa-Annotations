package org.forome.annotation.utils;

import java.util.HashSet;
import java.util.Set;

public class NucleotideUtils {

    private static Set<Character> nucleotides = new HashSet<Character>(){{
        add('A');//Аденин
        add('G');//Гуанин
        add('C');//Цитозин
        add('T');//Тимин
    }};

    public static boolean validation(char nucleotide) {
        return nucleotides.contains(nucleotide);
    }
}
