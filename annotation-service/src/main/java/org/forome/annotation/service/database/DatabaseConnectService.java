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

package org.forome.annotation.service.database;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.RocksDBProvider;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.forome.annotation.config.connector.base.DatabaseConfigConnector;
import org.forome.annotation.config.database.DatabaseConfig;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseConnectService implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(DatabaseConnectService.class);

	public static final String COLUMN_FAMILY_RECORD = "record";

	private final RocksDB rocksDB;
	private final Map<String, ColumnFamilyHandle> columnFamilies;

	private final SSHConnectService sshTunnelService;
	private final Map<String, ComboPooledDataSource> dataSources;

	public DatabaseConnectService(SSHConnectService sshTunnelService, DatabaseConfig databaseConfig) throws DatabaseException {
		this.sshTunnelService = sshTunnelService;
		this.dataSources = new HashMap<>();

		Path pathDatabase = databaseConfig.path;
		try (DBOptions options = buildOptions(pathDatabase)) {
			List<ColumnFamilyDescriptor> columnFamilyDescriptors = getColumnFamilyDescriptors(pathDatabase);

			List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
			rocksDB = RocksDB.openReadOnly(options, pathDatabase.toString(), columnFamilyDescriptors, columnFamilyHandles);

			columnFamilies = new HashMap<>();
			for (int i = 0; i < columnFamilyDescriptors.size(); i++) {
				String columnFamilyName = TypeConvert.unpackString(columnFamilyDescriptors.get(i).getName());
				ColumnFamilyHandle columnFamilyHandle = columnFamilyHandles.get(i);
				columnFamilies.put(columnFamilyName, columnFamilyHandle);
			}
		} catch (RocksDBException e) {
			throw new DatabaseException(e);
		}
	}

	public RocksDB getRocksDB() {
		return rocksDB;
	}

	public ColumnFamilyHandle getColumnFamily(String name) {
		return columnFamilies.get(name);
	}

	public Record getRecord(Position position) {
		int k = position.value / BatchRecord.DEFAULT_SIZE;
		Interval interval = Interval.of(
				position.chromosome,
				k * BatchRecord.DEFAULT_SIZE,
				k * BatchRecord.DEFAULT_SIZE + BatchRecord.DEFAULT_SIZE - 1
		);
		try {
			byte[] bytes = rocksDB.get(
					getColumnFamily(COLUMN_FAMILY_RECORD),
					new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(interval)
			);
			if (bytes == null) return null;

			BatchRecord batchRecord = new BatchRecord(interval, bytes);
			return batchRecord.getRecord(position);
		} catch (RocksDBException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}

	public ComboPooledDataSource getDataSource(DatabaseConfigConnector databaseConfigConnector) throws Exception {
		String keyDataSource = getKeyDataSource(databaseConfigConnector);
		ComboPooledDataSource dataSource = dataSources.get(keyDataSource);
		if (dataSource == null) {
			synchronized (dataSources) {
				dataSource = dataSources.get(keyDataSource);
				if (dataSource == null) {
					dataSource = connect(databaseConfigConnector);
					dataSources.put(keyDataSource, dataSource);
				}
			}
		}
		return dataSource;
	}

	private ComboPooledDataSource connect(DatabaseConfigConnector databaseConfigConnector) throws Exception {
		StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
				.append(databaseConfigConnector.mysqlHost).append(':');

		int mysqlUrlPort;
		SshTunnelConfig sshTunnelConfig = databaseConfigConnector.sshTunnelConfig;
		if (sshTunnelConfig != null) {
			SSHConnect sshTunnel = sshTunnelService.getSSHConnect(
					sshTunnelConfig.host,
					sshTunnelConfig.port,
					sshTunnelConfig.user,
					sshTunnelConfig.key
			);
			mysqlUrlPort = sshTunnel.getTunnel(databaseConfigConnector.mysqlPort);
		} else {
			mysqlUrlPort = databaseConfigConnector.mysqlPort;
		}
		jdbcUrl.append(mysqlUrlPort).append('/').append(databaseConfigConnector.mysqlDatabase)
				.append("?user=").append(databaseConfigConnector.mysqlUser)
				.append("&password=").append(databaseConfigConnector.mysqlPassword)
				.append("&useSSL=false");

		String driverName = "com.mysql.jdbc.Driver";
		Class.forName(driverName).newInstance();

		ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
		pooledDataSource.setDriverClass(driverName);
		pooledDataSource.setJdbcUrl(jdbcUrl.toString());
		pooledDataSource.setMinPoolSize(1);
		pooledDataSource.setAcquireIncrement(1);
		pooledDataSource.setMaxPoolSize(20);
		pooledDataSource.setCheckoutTimeout((int) Duration.ofMinutes(1).toMillis());
		pooledDataSource.setTestConnectionOnCheckin(false);
		pooledDataSource.setTestConnectionOnCheckout(true);

		log.debug("Database connected by: {}", databaseConfigConnector.mysqlHost);

		return pooledDataSource;
	}

	private static String getKeyDataSource(DatabaseConfigConnector databaseConfigConnector) {
		StringBuilder builderKey = new StringBuilder();
		if (databaseConfigConnector.sshTunnelConfig != null) {
			SshTunnelConfig sshTunnelConfig = databaseConfigConnector.sshTunnelConfig;
			String keySSHConnect = SSHConnectService.getKeySSHConnect(
					sshTunnelConfig.host, sshTunnelConfig.port, sshTunnelConfig.user
			);
			builderKey.append(keySSHConnect);
		}
		return builderKey.append(databaseConfigConnector.mysqlHost)
				.append(databaseConfigConnector.mysqlPort)
				.append(databaseConfigConnector.mysqlUser)
				.toString();
	}

	@Override
	public void close() {
		for (ComboPooledDataSource dataSource : dataSources.values()) {
			dataSource.close();
		}
	}

	private static DBOptions buildOptions(Path pathDatabase) throws RocksDBException {
		final String optionsFilePath = pathDatabase.toString() + ".ini";

		DBOptions options = new DBOptions();
		if (Files.exists(Paths.get(optionsFilePath))) {
			final List<ColumnFamilyDescriptor> ignoreDescs = new ArrayList<>();
			OptionsUtil.loadOptionsFromFile(optionsFilePath, Env.getDefault(), options, ignoreDescs, false);
		} else {
			options
					.setInfoLogLevel(InfoLogLevel.WARN_LEVEL)
					.setMaxTotalWalSize(100L * SizeUnit.MB);
		}

		return options.setCreateIfMissing(true);
	}

	private static List<ColumnFamilyDescriptor> getColumnFamilyDescriptors(Path pathDatabase) throws RocksDBException {
		List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();

		try (Options options = new Options()) {
			for (byte[] columnFamilyName : RocksDB.listColumnFamilies(options, pathDatabase.toString())) {
				columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName));
			}
		}

		if (columnFamilyDescriptors.isEmpty()) {
			columnFamilyDescriptors.add(new ColumnFamilyDescriptor(TypeConvert.pack(RocksDBProvider.DEFAULT_COLUMN_FAMILY)));
		}

		return columnFamilyDescriptors;
	}
}
