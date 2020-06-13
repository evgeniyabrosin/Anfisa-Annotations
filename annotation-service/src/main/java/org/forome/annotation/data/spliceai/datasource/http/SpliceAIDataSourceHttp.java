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

package org.forome.annotation.data.spliceai.datasource.http;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.nio.reactor.IOReactorException;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.spliceai.datasource.SpliceAIDataSource;
import org.forome.annotation.data.spliceai.struct.Row;
import org.forome.annotation.struct.*;
import org.forome.annotation.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpliceAIDataSourceHttp implements SpliceAIDataSource {

	private final static Logger log = LoggerFactory.getLogger(SpliceAIDataSourceHttp.class);

	private final LiftoverConnector liftoverConnector;

	public SpliceAIDataSourceHttp(
			LiftoverConnector liftoverConnector
	) throws IOReactorException {
		this.liftoverConnector = liftoverConnector;

	}

	@Override
	public List<Row> getAll(AnfisaExecuteContext context, Assembly assembly, String chromosome, int position, String ref, Allele altAllele) {
		Position pos38 = liftoverConnector.toHG38(assembly, new Position(Chromosome.of(chromosome), position));
		if (pos38 == null) {
			return Collections.emptyList();
		}

		JSONObject response = context.sourceSpliceAI_and_dbNSFP;
		JSONArray jRecords = (JSONArray) response.get("SpliceAI");
		if (jRecords == null) {
			return Collections.emptyList();
		}

		List<JSONObject> records = jRecords.stream()
				.map(o -> (JSONObject) o)
				.filter(item -> item.getAsString("REF").equals(ref) && item.getAsString("ALT").equals(altAllele.getBaseString()))
				.collect(Collectors.toList());

		return records.stream().map(jsonObject -> _build(pos38, jsonObject)).collect(Collectors.toList());
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return Collections.emptyList();
	}

	@Override
	public void close() {

	}

	private static Row _build(Position pos38, JSONObject jsonObject) {
		String chrom = pos38.chromosome.getChar();
		int pos = pos38.value;
		String ref = jsonObject.getAsString("REF");
		String alt = jsonObject.getAsString("ALT");
		String symbol = jsonObject.getAsString("SYMBOL");
		String strand = "-";
		String type = "-";
		int dp_ag = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_AG"));
		int dp_al = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_AL"));
		int dp_dg = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_DG"));
		int dp_dl = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_DL"));
		float ds_ag = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_AG"));
		float ds_al = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_AL"));
		float ds_dg = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_DG"));
		float ds_dl = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_DL"));
		String id = jsonObject.getAsString("ID");
		float max_ds = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("MAX_DS"));

		return new Row(
				chrom, pos, ref, alt,
				symbol, strand, type,
				dp_ag, dp_al, dp_dg, dp_dl,
				ds_ag, ds_al, ds_dg, ds_dl,
				id, max_ds
		);
	}
}
