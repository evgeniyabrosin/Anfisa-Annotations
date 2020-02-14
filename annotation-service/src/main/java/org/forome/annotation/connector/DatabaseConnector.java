package org.forome.annotation.connector;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.resourcepool.TimeoutException;
import org.forome.annotation.config.connector.base.DatabaseConfigConnector;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector implements Closeable {

	public static class Metadata {

		public final String product;
		public final String version;
		public final Instant date;

		public Metadata(String product, String version, Instant date) {
			this.product = product;
			this.version = version;
			this.date = date;
		}
	}

	private final static Logger log = LoggerFactory.getLogger(DatabaseConnector.class);

	private final DatabaseConfigConnector databaseConfigConnector;
	private final ComboPooledDataSource pooledDataSource;

	public DatabaseConnector(DatabaseConnectService databaseConnectService, DatabaseConfigConnector databaseConfigConnector) throws Exception {
		this.databaseConfigConnector = databaseConfigConnector;
		this.pooledDataSource = databaseConnectService.getDataSource(databaseConfigConnector);
	}

	public Connection createConnection() {
		int attempt = 0;
		while (true) {
			try {
				return pooledDataSource.getConnection();
			} catch (SQLException e) {
				if (e.getCause() instanceof TimeoutException) {
					//at com.mchange.v2.resourcepool.BasicResourcePool.awaitAvailable(BasicResourcePool.java:1467)
					attempt++;
					if (attempt > 120) {
						log.debug("Exception", e);
						throw ExceptionBuilder.buildExternalDatabaseException(e);
					} else {
						log.debug("Failed create connect... pause, attempt: {}", attempt);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException ignore) {
						}
						continue;
					}
				} else {
					log.debug("Exception", e);
					throw ExceptionBuilder.buildExternalDatabaseException(e);
				}
			}
		}
	}

	public List<Metadata> getMetadata() {
		String sql = String.format("select Product, Version, Date from %s.Metadata", databaseConfigConnector.mysqlDatabase);

		List<Metadata> metadata = new ArrayList<>();
		try (Connection connection = createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String product = resultSet.getString("Product");
						String version = resultSet.getString("Version");
						Date date = resultSet.getDate("Date");//YYYY-MM-dd
						metadata.add(new Metadata(
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
