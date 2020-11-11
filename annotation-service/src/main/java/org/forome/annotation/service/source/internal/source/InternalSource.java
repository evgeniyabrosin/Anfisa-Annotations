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

package org.forome.annotation.service.source.internal.source;

import net.minidev.json.JSONArray;
import org.forome.annotation.service.source.external.source.ExternalSource;
import org.forome.annotation.service.source.internal.fasta.FastaSourcePortPython;
import org.forome.annotation.service.source.struct.Record;
import org.forome.annotation.service.source.struct.Source;
import org.forome.astorage.core.data.Conservation;
import org.forome.astorage.pastorage.PAStorage;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;

public class InternalSource implements Source {

	private final Assembly assembly;

	private final PAStorage paStorage;
	private final org.forome.astorage.core.source.Source source;

	public final ExternalSource externalSource;

	public InternalSource(Assembly assembly, PAStorage paStorage, org.forome.astorage.core.source.Source source, ExternalSource externalSource) {
		this.assembly = assembly;
		this.paStorage = paStorage;
		this.source = source;
		this.externalSource = externalSource;
	}

	@Override
	public Record getRecord(Position position) {
		return externalSource.getRecord(position);
	}

	@Override
	public Sequence getFastaSequence(Interval interval) {
		FastaSourcePortPython fastaSourcePortPython = new FastaSourcePortPython(paStorage);
		return fastaSourcePortPython.getSequence(assembly, interval);
	}

	@Override
	public JSONArray getGnomad(Position pos37) {
		return externalSource.getGnomad(pos37);
	}

	@Override
	public JSONArray getDbSNP(Interval interval) {
		return externalSource.getDbSNP(interval);
	}

	@Override
	public JSONArray getDbNSFP(Interval interval) {
		return externalSource.getDbNSFP(interval);
	}

	@Override
	public JSONArray getSpliceAI(Interval interval) {
		return externalSource.getSpliceAI(interval);
	}

	@Override
	public Conservation getConservation(Position position) {
		return externalSource.getConservation(position);
	}
}
