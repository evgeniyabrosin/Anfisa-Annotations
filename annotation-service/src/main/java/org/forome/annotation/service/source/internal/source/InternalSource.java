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
import org.forome.annotation.service.source.internal.common.CommonSourcePortPython;
import org.forome.annotation.service.source.internal.fasta.FastaSourcePortPython;
import org.forome.annotation.service.source.struct.Record;
import org.forome.annotation.service.source.struct.Source;
import org.forome.astorage.core.data.Conservation;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.astorage.pastorage.PAStorage;
import org.forome.astorage.pastorage.schema.SchemaCommon;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;

import java.io.IOException;

public class InternalSource implements Source {

	private final Assembly assembly;

	private final PAStorage paStorage;
	private final org.forome.astorage.core.source.Source source;

	public final LiftoverConnector liftoverConnector;

	public final ExternalSource externalSource;

	public InternalSource(Assembly assembly, PAStorage paStorage, org.forome.astorage.core.source.Source source, ExternalSource externalSource) {
		this.assembly = assembly;
		this.paStorage = paStorage;
		this.source = source;

		try {
			this.liftoverConnector = new LiftoverConnector();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.externalSource = externalSource;

//		if (assembly == Assembly.GRCh37) {
//			getGnomad(new Position(Chromosome.CHR_18, 67760501));
//		}
		if (assembly == Assembly.GRCh37) {
			getGnomad(new Position(Chromosome.CHR_12, 885081));
		}
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
		//TODO Ulitin V. В этот метод приходят сразу hg37 координаты, хотя они должны конвертироваться тут
		//		Position position37 = liftoverConnector.toHG37(assembly, position);
//		if (position37 == null) {
//			return new JSONArray();
//		}
		CommonSourcePortPython сommonSourcePortPython = new CommonSourcePortPython(paStorage);
		return сommonSourcePortPython.get(SchemaCommon.SCHEMA_GNOMAD_NAME, Assembly.GRCh37, Interval.of(pos37));
	}

	@Override
	public JSONArray getDbSNP(Interval interval) {
		Interval interval38 = liftoverConnector.toHG38(assembly, interval);
		if (interval38 == null) {
			return new JSONArray();
		}
		CommonSourcePortPython сommonSourcePortPython = new CommonSourcePortPython(paStorage);
		return сommonSourcePortPython.get(SchemaCommon.SCHEMA_DBSNP_NAME, Assembly.GRCh38, interval38);
	}

	@Override
	public JSONArray getDbNSFP(Interval interval) {
		Interval interval38 = liftoverConnector.toHG38(assembly, interval);
		if (interval38 == null) {
			return new JSONArray();
		}
		CommonSourcePortPython сommonSourcePortPython = new CommonSourcePortPython(paStorage);
		return сommonSourcePortPython.get(SchemaCommon.SCHEMA_DBNSFP_NAME, Assembly.GRCh38, interval38);
	}

	@Override
	public JSONArray getSpliceAI(Interval interval) {
		return externalSource.getSpliceAI(interval);
	}

	@Override
	public Conservation getConservation(Position position) {
//		Position position37 = liftoverConnector.toHG37(assembly, position);
//		if (position37 == null) {
//			return new Conservation(null, null);
//		}
//
//		CommonSourcePortPython сommonSourcePortPython = new CommonSourcePortPython(paStorage);
//		JSONArray results = сommonSourcePortPython.get(SchemaCommon.SCHEMA_GERP_NAME, Assembly.GRCh37, Interval.of(position37));
//
//		if (results.isEmpty()) {
//			return new Conservation(null, null);
//		} else if (results.size()>1) {
//			throw new RuntimeException();
//		} else {
//			JSONObject result = new JSONObject();
//			result = (JSONObject) result.get(0);
//
//			JSONObject jGerp = (JSONObject) result.get("Gerp");
//			if (jGerp == null) {
//				return new Conservation(null, null);
//			} else {
//				Float gerpRS = ConservationHttpRequest.toFloat(jGerp.getAsNumber("GerpRS"));
//				Float gerpN = ConservationHttpRequest.toFloat(jGerp.getAsNumber("GerpN"));
//
//				return new Conservation(gerpRS, gerpN);
//			}
//		}

		return externalSource.getConservation(position);
	}
}
