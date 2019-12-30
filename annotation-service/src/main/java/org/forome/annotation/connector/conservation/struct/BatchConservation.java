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

package org.forome.annotation.connector.conservation.struct;

import org.forome.annotation.struct.Interval;

public class BatchConservation {

	public final Interval interval;
	public final ConservationItem[] items;

	public BatchConservation(Interval interval, ConservationItem[] items) {
		this.interval = interval;
		this.items = items;
	}

	public ConservationItem getConservation(int position) {
		int index = position - interval.start;
		return items[index];
	}

}
