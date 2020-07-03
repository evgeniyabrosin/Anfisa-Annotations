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

import java.util.Objects;

public class Position {

	public final Chromosome chromosome;

	public final int value;

	public Position(Chromosome chromosome, int value) {
		this.chromosome = chromosome;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Position position = (Position) o;
		return value == position.value &&
				Objects.equals(chromosome, position.chromosome);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chromosome, value);
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("Position(")
				.append("chr: ").append(chromosome.getChar())
				.append(", value: ").append(value)
				.append(')')
				.toString();
	}
}
