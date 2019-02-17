package ru.processtech.forome.annotation.controller.utils;

import com.google.common.base.Strings;
import ru.processtech.forome.annotation.exception.ExceptionBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestParser {

	private static Set<String> CHROMOSOME_VALIDE_CHARS = new HashSet<String>(
			Arrays.asList("M", "X", "Y")
	);

	public static String toChromosome(String value) {
		if (Strings.isNullOrEmpty(value)) {
			throw ExceptionBuilder.buildInvalidValueException("chromosome", value);
		}

		if (CHROMOSOME_VALIDE_CHARS.contains(value)) {
			return value;
		}

		if (value.startsWith("chr")) {
			value = value.substring("chr".length());
		}

		try {
			int number = Integer.parseInt(value);
			if (number < 1 || number > 23) {
				throw ExceptionBuilder.buildInvalidValueException("chromosome", value);
			}
			return value;
		} catch (Throwable ex) {
			throw ExceptionBuilder.buildInvalidValueException("chromosome", value);
		}
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
}
