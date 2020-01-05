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
import com.google.common.collect.ImmutableList;
import org.forome.annotation.exception.ExceptionBuilder;

import java.util.Objects;

public class Chromosome {

	public static final Chromosome CHR_1 = new Chromosome("1");
	public static final Chromosome CHR_2 = new Chromosome("2");
	public static final Chromosome CHR_3 = new Chromosome("3");
	public static final Chromosome CHR_4 = new Chromosome("4");
	public static final Chromosome CHR_5 = new Chromosome("5");
	public static final Chromosome CHR_6 = new Chromosome("6");
	public static final Chromosome CHR_7 = new Chromosome("7");
	public static final Chromosome CHR_8 = new Chromosome("8");
	public static final Chromosome CHR_9 = new Chromosome("9");
	public static final Chromosome CHR_10 = new Chromosome("10");
	public static final Chromosome CHR_11 = new Chromosome("11");
	public static final Chromosome CHR_12 = new Chromosome("12");
	public static final Chromosome CHR_13 = new Chromosome("13");
	public static final Chromosome CHR_14 = new Chromosome("14");
	public static final Chromosome CHR_15 = new Chromosome("15");
	public static final Chromosome CHR_16 = new Chromosome("16");
	public static final Chromosome CHR_17 = new Chromosome("17");
	public static final Chromosome CHR_18 = new Chromosome("18");
	public static final Chromosome CHR_19 = new Chromosome("19");
	public static final Chromosome CHR_20 = new Chromosome("20");
	public static final Chromosome CHR_21 = new Chromosome("21");
	public static final Chromosome CHR_22 = new Chromosome("22");
	public static final Chromosome CHR_23 = new Chromosome("23");
	public static final Chromosome CHR_X = new Chromosome("X");
	public static final Chromosome CHR_Y = new Chromosome("Y");

	public static final ImmutableList<Chromosome> CHROMOSOMES = ImmutableList.of(
			CHR_1, CHR_2, CHR_3, CHR_4, CHR_5, CHR_6, CHR_7, CHR_8, CHR_9,
			CHR_10, CHR_11, CHR_12, CHR_13, CHR_14, CHR_15, CHR_16, CHR_17, CHR_18, CHR_19,
			CHR_20, CHR_21, CHR_22, CHR_23,
			CHR_X, CHR_Y
	);

	private final String value;

	private Chromosome(String value) {
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

		for (Chromosome chromosome : CHROMOSOMES) {
			if (value.equals(chromosome.value)) {
				return chromosome;
			}
		}

		if (value.startsWith("PATCH_")) {
			return new Chromosome(value);
		} else {
			throw ExceptionBuilder.buildInvalidChromosome(str);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Chromosome that = (Chromosome) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	public static boolean isSupportChromosome(String str) {
		Chromosome chromosome = Chromosome.of(str);
		return CHROMOSOMES.contains(chromosome);
	}
}
