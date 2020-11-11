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

package org.forome.annotation.service.source.external.source;

import net.minidev.json.JSONArray;
import org.apache.http.nio.reactor.IOReactorException;
import org.forome.annotation.service.source.external.ExternalDataSource;
import org.forome.annotation.service.source.external.conservation.ConservationHttpRequest;
import org.forome.annotation.service.source.external.dbSNP.DbSNPHttpRequest;
import org.forome.annotation.service.source.external.fasta.FastaHttpRequest;
import org.forome.annotation.service.source.external.gnomad.GnomadHttpRequest;
import org.forome.annotation.service.source.struct.Record;
import org.forome.annotation.service.source.struct.Source;
import org.forome.astorage.core.data.Conservation;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;

import java.net.URISyntaxException;

public class ExternalSource implements Source {

	public final ExternalDataSource httpDataSource;
	public final Assembly assembly;

	public ExternalSource(ExternalDataSource httpDataSource, Assembly assembly) {
		this.httpDataSource = httpDataSource;
		this.assembly = assembly;
	}

	@Override
	public Record getRecord(Position position) {
		throw new RuntimeException();
	}

	@Override
	public Sequence getFastaSequence(Interval interval) {
		try {
			FastaHttpRequest fastaSourcePython = new FastaHttpRequest(httpDataSource.url);
			return fastaSourcePython.getSequence(assembly, interval);
		} catch (IOReactorException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONArray getGnomad(Position position) {
		GnomadHttpRequest gnomadHttpRequest = new GnomadHttpRequest(this);
		return gnomadHttpRequest.get(position);
	}

	@Override
	public JSONArray getDbSNP(Interval interval) {
		try {
			DbSNPHttpRequest dbSNPHttpRequest = new DbSNPHttpRequest(this);
			return dbSNPHttpRequest.get(interval);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Conservation getConservation(Position position) {
		ConservationHttpRequest conservationHttpRequest = new ConservationHttpRequest(this);
		return conservationHttpRequest.getConservation(position);
	}
}
