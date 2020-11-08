/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.service.source.struct.source.internal;

import org.forome.annotation.service.source.struct.Record;
import org.forome.annotation.service.source.struct.source.Source;
import org.forome.annotation.service.source.struct.source.http.HttpSource;
import org.forome.annotation.service.source.tmp.GnomadDataResponse;
import org.forome.astorage.core.data.Conservation;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;

public class InternalSource implements Source {

	private final org.forome.astorage.core.source.Source source;
	public final HttpSource httpSource;

	public InternalSource(org.forome.astorage.core.source.Source source, HttpSource httpSource) {
		this.source = source;
		this.httpSource = httpSource;
	}

	@Override
	public Record getRecord(Position position) {
		return httpSource.getRecord(position);
	}

	@Override
	public Sequence getFastaSequence(Interval interval) {
		return httpSource.getFastaSequence(interval);
	}

	@Override
	public GnomadDataResponse getGnomad(Position pos37) {
		return httpSource.getGnomad(pos37);
	}

	@Override
	public Conservation getConservation(Position position) {
		return getRecord(position).getConservation();
	}
}
