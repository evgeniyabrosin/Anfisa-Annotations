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

package org.forome.annotation.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics {

	public class Stat {

		public final int count;
		public final long fullNanoTime;
		public final double averageNanoTime;

		public Stat(int count, long fullNanoTime) {
			this.count = count;
			this.fullNanoTime = fullNanoTime;
			this.averageNanoTime = fullNanoTime / (float) count;
		}

		@Override
		public String toString() {
			return "Stat{" +
					"count=" + count +
					", fullNanoTime=" + fullNanoTime +
					", averageNanoTime=" + averageNanoTime +
					'}';
		}
	}

	private final AtomicInteger count;
	private final AtomicLong time;

	public Statistics() {
		this.count = new AtomicInteger();
		this.time = new AtomicLong();
	}

	public void addTime(long timeNano) {
		count.incrementAndGet();
		time.addAndGet(timeNano);
	}

	public Stat getStat() {
		return new Stat(
				count.get(), time.get()
		);
	}
}
