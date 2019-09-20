package org.forome.annotation.controller.utils;

import com.google.common.base.Strings;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Chromosome;

public class RequestParser {

    public static String toChromosome(String value) {
        Chromosome chromosome = Chromosome.of(value);
        return chromosome.getChar();
    }

    public static long toLong(String param, String value) {
        if (Strings.isNullOrEmpty(value)) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }
        try {
            return Long.parseLong(value);
        } catch (Throwable ex) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }
    }

    public static int toInteger(String param, String value) {
        if (Strings.isNullOrEmpty(value)) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }
        try {
            return Integer.parseInt(value);
        } catch (Throwable ex) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }
    }

    public static String toString(String param, String value) {
        if (value == null) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }

        String result = value.trim();
        if (result.isEmpty()) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }

        return result;
    }

    public static <T> T toObject(String param, Object value) {
        if (value == null) {
            throw ExceptionBuilder.buildInvalidValueException(param, value);
        }
        return (T) value;
    }
}
