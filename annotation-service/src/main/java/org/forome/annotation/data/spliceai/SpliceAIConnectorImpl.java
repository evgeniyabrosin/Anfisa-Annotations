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

package org.forome.annotation.data.spliceai;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.spliceai.datasource.SpliceAIDataSource;
import org.forome.annotation.data.spliceai.struct.Row;
import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.core.struct.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpliceAIConnectorImpl implements SpliceAIConnector, AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(SpliceAIConnectorImpl.class);

	public final static float MAX_DS_UNLIKELY = 0.2f;

	private final SpliceAIDataSource spliceAIDataSource;

//	private final DatabaseConnector databaseConnector;
//
//	private final SpliceAIDataConnector spliceAIDataConnector;

	public SpliceAIConnectorImpl(
			SpliceAIDataSource spliceAIDataSource
//			DatabaseConnectService databaseConnectService,
//			SpliceAIConfigConnector spliceAIConfigConnector
	) throws Exception {
		this.spliceAIDataSource = spliceAIDataSource;

//		databaseConnector = new DatabaseConnector(databaseConnectService, spliceAIConfigConnector);
//		spliceAIDataConnector = new SpliceAIDataConnector(databaseConnector);
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return spliceAIDataSource.getSourceMetadata();
	}

	@Override
	public SpliceAIResult getAll(Source source, AnfisaExecuteContext context, Assembly assembly, String chromosome, int position, String ref, Allele altAllele) {
		List<Row> rows = spliceAIDataSource.getAll(source, context, assembly, chromosome, position, ref, altAllele);

		String cases;
		float max_ds;
		Map<String, SpliceAIResult.DictSql> dict_sql = new HashMap<>();
		if (rows.isEmpty()) {
			cases = "None";
			max_ds = 0;
		} else {
			max_ds = rows.stream().map(row -> row.max_ds).max(Float::compareTo).orElse(0.0f);
			if (max_ds < SpliceAIConnectorImpl.MAX_DS_UNLIKELY) {
				cases = "unlikely";
			} else if (max_ds < 0.5f) {
				cases = "likely_pathogenic";
			} else if (max_ds < 0.8f) {
				cases = "pathogenic";
			} else if (max_ds <= 1.0f) {
				cases = "high_precision_pathogenic";
			} else {
				throw new RuntimeException("Not support value max_ds: " + max_ds);
			}
			for (Row row : rows) {
				dict_sql.put(
						String.format("%s/%s/%s/%s", row.alt, row.symbol, row.strand, row.type),
						new SpliceAIResult.DictSql(
								row.dp_ag, row.dp_al, row.dp_dg, row.dp_dl,
								row.ds_ag, row.ds_al, row.ds_dg, row.ds_dl
						)
				);
			}
		}
		return new SpliceAIResult(cases, max_ds, dict_sql);
	}

	@Override
	public void close() {
		spliceAIDataSource.close();
	}

}
