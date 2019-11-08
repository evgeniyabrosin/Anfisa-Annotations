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

public class Position<T> {

	public final T start;
	public final T end;

	public Position(T position) {
		this(position, position);
	}

	public Position(T start, T end) {
		if (start == null) throw new IllegalArgumentException();
		if (end == null) throw new IllegalArgumentException();

		this.start = start;
		this.end = end;
	}

	public boolean isSingle() {
		return Objects.equals(start, end);
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder()
				.append("Position(");
		if (isSingle()) {
			sBuilder.append(start);
		} else {
			sBuilder.append("start: ").append(start);
			sBuilder.append(", end: ").append(end);
		}
		sBuilder.append(')');
		return sBuilder.toString();
	}
}
