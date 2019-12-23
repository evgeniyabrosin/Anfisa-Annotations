/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.struct;

import com.google.common.base.Strings;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;

public enum Chromosome {

	CHR_1("1"),
	CHR_2("2"),
	CHR_3("3"),
	CHR_4("4"),
	CHR_5("5"),
	CHR_6("6"),
	CHR_7("7"),
	CHR_8("8"),
	CHR_9("9"),
	CHR_10("10"),
	CHR_11("11"),
	CHR_12("12"),
	CHR_13("13"),
	CHR_14("14"),
	CHR_15("15"),
	CHR_16("16"),
	CHR_17("17"),
	CHR_18("18"),
	CHR_19("19"),
	CHR_20("20"),
	CHR_21("21"),
	CHR_22("22"),
	CHR_23("23"),
	CHR_X("X"),
	CHR_Y("Y");

	private final String value;

	Chromosome(String value) {
		this.value = value;
	}

	/**
	 * Короткая запись
	 *
	 * @return
	 */
	public String getChar() {
		return value;
	}

	/**
	 * Полная запись
	 *
	 * @return
	 */
	public String getChromosome() {
		return toString();
	}

	@Override
	public String toString() {
		return String.format("chr%s", value.toUpperCase());
	}

	public static Chromosome of(String str) {
		if (Strings.isNullOrEmpty(str)) {
			throw ExceptionBuilder.buildInvalidValueException("chromosome", str);
		}

		String value = str.toUpperCase();
		if (value.startsWith("CHR")) {
			value = value.substring("CHR".length());
		}

		for (Chromosome chromosome : Chromosome.values()) {
			if (value.equals(chromosome.getChar())) {
				return chromosome;
			}
		}

		throw ExceptionBuilder.buildInvalidChromosome(str);
	}

	public static boolean isChromosome(String str) {
		try {
			Chromosome.of(str);
			return true;
		} catch (AnnotatorException e) {
			if (ExceptionBuilder.CODE_INVALID_CHROMOSOME.equals(e.getCode())) {
				return false;
			}
			throw e;
		}
	}
}
