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

package org.forome.annotation.data.spliceai.datasource.mysql;

import com.google.common.collect.ImmutableList;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.spliceai.datasource.SpliceAIDataSource;
import org.forome.annotation.data.spliceai.struct.Row;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.SourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SpliceAIDataConnector implements SpliceAIDataSource, Closeable {

	private static final Logger log = LoggerFactory.getLogger(SpliceAIDataConnector.class);

	private final DatabaseConnector databaseConnector;

	private static final String TABLE = "spliceai.SPLICEAI";

	public static final ImmutableList<String> KEY_COLUMNS = ImmutableList.of(
			"CHROM",
			"POS",
			"REF",
			"ALT"
	);

	public static final ImmutableList<String> TARGET_COLUMNS = ImmutableList.of(
			"SYMBOL",
			"STRAND",
			"TYPE",
			"DP_AG",
			"DP_AL",
			"DP_DG",
			"DP_DL",
			"DS_AG",
			"DS_AL",
			"DS_DG",
			"DS_DL",
			"ID",
			"MAX_DS"
	);

	private static final ImmutableList<String> COLUMNS =
			new ImmutableList.Builder()
					.addAll(KEY_COLUMNS)
					.addAll(TARGET_COLUMNS)
					.build();


	public SpliceAIDataConnector(DatabaseConnector databaseConnector) {
		this.databaseConnector = databaseConnector;
	}

	@Override
	public List<Row> getAll(Assembly assembly, String chromosome, int position, String ref, Allele altAllele) {
		if (assembly != Assembly.GRCh37) {
			throw new RuntimeException("Not implemented");
		}

		String select_list = String.join(", ", COLUMNS);
		String sql = String.format("SELECT DISTINCT %s FROM %s WHERE CHROM = '%s' AND POS = %s AND REF = '%s' AND ALT = '%s'",
				select_list, TABLE, chromosome, position, ref, altAllele.getBaseString()
		);
		List<Row> rows = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						rows.add(_build(resultSet));
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}

		return rows;
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return databaseConnector.getSourceMetadata();
	}

	private static Row _build(ResultSet resultSet) throws SQLException {
		String chrom = resultSet.getString("CHROM");
		int pos = resultSet.getInt("POS");
		String ref = resultSet.getString("REF");
		String alt = resultSet.getString("ALT");
		String symbol = resultSet.getString("SYMBOL");
		String strand = resultSet.getString("STRAND");
		String type = resultSet.getString("TYPE");
		int dp_ag = resultSet.getInt("DP_AG");
		int dp_al = resultSet.getInt("DP_AL");
		int dp_dg = resultSet.getInt("DP_DG");
		int dp_dl = resultSet.getInt("DP_DL");
		float ds_ag = resultSet.getFloat("DS_AG");
		float ds_al = resultSet.getFloat("DS_AL");
		float ds_dg = resultSet.getFloat("DS_DG");
		float ds_dl = resultSet.getFloat("DS_DL");
		String id = resultSet.getString("ID");
		float max_ds = resultSet.getFloat("MAX_DS");

		return new Row(
				chrom, pos, ref, alt,
				symbol, strand, type,
				dp_ag, dp_al, dp_dg, dp_dl,
				ds_ag, ds_al, ds_dg, ds_dl,
				id, max_ds
		);
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
