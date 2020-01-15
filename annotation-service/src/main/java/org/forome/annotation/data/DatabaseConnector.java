/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.forome.annotation.config.connector.base.DatabaseConfigConnector;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.SourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector implements Closeable {

	private final static Logger log = LoggerFactory.getLogger(DatabaseConnector.class);

	private final DatabaseConfigConnector databaseConfigConnector;
	private final ComboPooledDataSource pooledDataSource;

	public DatabaseConnector(DatabaseConnectService databaseConnectService, DatabaseConfigConnector databaseConfigConnector) {
		this.databaseConfigConnector = databaseConfigConnector;
		this.pooledDataSource = databaseConnectService.getDataSource(databaseConfigConnector);
	}

	public Connection createConnection() {
		try {
			return pooledDataSource.getConnection();
		} catch (SQLException e) {
			log.debug("Exception", e);
			throw ExceptionBuilder.buildOperationException(e);
		}
	}

	public String getDatabase() {
		return databaseConfigConnector.mysqlDatabase;
	}

	public List<SourceMetadata> getSourceMetadata() {
		String sql = String.format("select Product, Version, Date from %s.Metadata", getDatabase());

		List<SourceMetadata> metadata = new ArrayList<>();
		try (Connection connection = createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String product = resultSet.getString("Product");
						String version = resultSet.getString("Version");
						Date date = resultSet.getDate("Date");//YYYY-MM-dd
						metadata.add(new SourceMetadata(
								product,
								version,
								(date != null) ? Instant.ofEpochMilli(date.getTime()) : null)
						);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return metadata;
	}

	@Override
	public void close() {

	}
}
