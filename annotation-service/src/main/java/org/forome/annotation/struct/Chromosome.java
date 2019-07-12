package org.forome.annotation.struct;

import com.google.common.base.Strings;
import org.forome.annotation.exception.ExceptionBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Chromosome {

    private static Set<String> CHROMOSOME_VALIDE_CHARS = new HashSet<String>(
            Arrays.asList("M", "X", "Y")
    );

    private final String value;

    public Chromosome(String str) {
        if (Strings.isNullOrEmpty(str)) {
            throw ExceptionBuilder.buildInvalidValueException("chromosome", str);
        }

        if (str.startsWith("chr")) {
            str = str.substring("chr".length());
        }

        if (CHROMOSOME_VALIDE_CHARS.contains(str)) {
            this.value = str;
        } else {
            try {
                int number = Integer.parseInt(str);
                if (number < 1 || number > 23) {
                    throw ExceptionBuilder.buildInvalidValueException("chromosome", str);
                }
                this.value = str;
            } catch (Throwable ex) {
                throw ExceptionBuilder.buildInvalidValueException("chromosome", str);
            }
        }
    }

    /**
     * Короткая запись
     * @return
     */
    public String getChar(){
        return value;
    }

    /**
     * Полная запись
     * @return
     */
    public String getChromosome(){
        return toString();
    }

    @Override
    public String toString() {
        return String.format("chr%s", value.toUpperCase());
    }
}
