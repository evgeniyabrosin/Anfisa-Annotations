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

package org.forome.annotation.data.gnomad.datasource.mysql;

import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gnomad.datasource.GnomadDataSource;
import org.forome.annotation.data.gnomad.struct.DataResponse;
import org.forome.annotation.data.gnomad.utils.GnomadUtils;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.SourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.*;

public class GnomadDataConnector implements GnomadDataSource, Closeable {

	private static final Logger log = LoggerFactory.getLogger(GnomadDataConnector.class);

	private static final String TABLE = "gnom2.VARIANTS";

	private final DatabaseConnector databaseConnector;

	public GnomadDataConnector(DatabaseConnector databaseConnector) {
		this.databaseConnector = databaseConnector;
	}

	public List<SourceMetadata> getSourceMetadata() {
		return databaseConnector.getSourceMetadata();
	}

	@Override
	public List<DataResponse> getData(
			AnfisaExecuteContext context,
			Assembly assembly,
			Chromosome chromosome,
			int position,
			String ref,
			String alt,
			String fromWhat
	) {
		if (assembly != Assembly.GRCh37) {
			throw new RuntimeException("Not implemented");
		}

		boolean isSNV = (ref.length() == 1 && alt.length() == 1);

		String base_sql = String.format(
				"SELECT * FROM %s WHERE CHROM = '%%s' and POS = %%s",
				TABLE
		);

		String sql;
		if (isSNV) {
			sql = base_sql + " and REF = '%s' and ALT = '%s'";
		} else {
			sql = base_sql + " and (REF LIKE '%%%s%%' and ALT LIKE '%%%s%%')";
		}

		if (fromWhat != null) {
			List<String> q = Arrays.asList(fromWhat.toLowerCase().split(","));
			String s;
			if (q.size() == 1) {
				if (q.get(0).contains("exome")) {
					s = "e";
				} else if (q.get(0).contains("genome")) {
					s = "g";
				} else {
					s = q.get(0);
				}
			} else {
				throw new RuntimeException("Not support many s");
			}
			if (!(q.contains("e") && q.contains("g"))) {
				sql = String.format("%s and `SOURCE` = '%s'", sql, s);
			}
		}

		try (Connection connection = databaseConnector.createConnection()) {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(String.format(sql, chromosome.getChar(), position, ref, alt));
			boolean isEmptyResultSet = !resultSet.next();

			if (!isSNV && isEmptyResultSet) {
				resultSet.close();
				statement.close();

				statement = connection.createStatement();
				resultSet = statement.executeQuery(
						String.format(base_sql, chromosome.getChar(), position - 1)
				);
			}

			List<DataResponse> results = new ArrayList<>();
			resultSet.beforeFirst();
			while (resultSet.next()) {
				String diff_ref_alt = GnomadUtils.diff(ref, alt);
				if (Objects.equals(diff_ref_alt, GnomadUtils.diff(resultSet.getString("REF"), resultSet.getString("ALT")))
						||
						GnomadUtils.diff3(resultSet.getString("REF"), resultSet.getString("ALT"), diff_ref_alt)
				) {
					results.add(build(resultSet));
				}
			}

			resultSet.close();
			statement.close();

			return results;
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}

	@Override
	public void close() {
		databaseConnector.close();
	}

	private static DataResponse build(ResultSet resultSet) throws SQLException {
		Map<String, Object> columns = new HashMap<>();
		ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
		for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
			String columnName = resultSetMetaData.getColumnName(i).toLowerCase();
			columns.put(columnName, resultSet.getObject(i));
		}
		return new DataResponse(columns);
	}

}
