/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.struct;

import java.util.Objects;

/**
 * При использовании этого класса принято соглашения, что границы входят в наш интервал,
 * т.е. при start: 2 и end: 4, мы имеет 3 позиции: 2, 3, 4,
 * за исключением инсерций, в этой ситуации, расположение межд этими интервалами
 */
public class Interval {

	public final Chromosome chromosome;

	public final int start;
	public final int end;

	private Interval(Chromosome chromosome, int start, int end) {
		this.chromosome = chromosome;
		this.start = start;
		this.end = end;
	}

	public boolean isSingle() {
		return start == end;
	}

	public int getMin() {
		return Math.min(start, end);
	}

	public int getMax() {
		return Math.max(start, end);
	}

	public boolean contains(Position position) {
		if (!chromosome.equals(position.chromosome)) return false;
		return (position.value >= start && position.value <= end);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Interval interval = (Interval) o;
		return chromosome == interval.chromosome &&
				start == interval.start &&
				end == interval.end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(chromosome, start, end);
	}

	public static Interval of(Position position) {
		return of(position.chromosome, position.value);
	}

	public static Interval of(Chromosome chromosome, int position) {
		return of(chromosome, position, position);
	}

	public static Interval of(Chromosome chromosome, int start, int end) {
		if (start > end && start - 1 != end) {
			throw new IllegalArgumentException();
		}
		return new Interval(chromosome, start, end);
	}

	public static Interval ofWithoutValidation(Chromosome chromosome, int start, int end) {
		return new Interval(chromosome, start, end);
	}
}

