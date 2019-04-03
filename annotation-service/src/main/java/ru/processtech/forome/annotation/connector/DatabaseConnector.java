package ru.processtech.forome.annotation.connector;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.processtech.forome.annotation.config.connector.database.DatabaseConfigConnector;
import ru.processtech.forome.annotation.config.sshtunnel.SshTunnelConfig;
import ru.processtech.forome.annotation.exception.ExceptionBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

public class DatabaseConnector implements Closeable {

	private final static Logger log = LoggerFactory.getLogger(DatabaseConnector.class);

	private final DatabaseConfigConnector databaseConfigConnector;

	private JSch jsch;
	private Session sshSession;

	private ComboPooledDataSource pooledDataSource;

	public DatabaseConnector(DatabaseConfigConnector databaseConfigConnector) throws Exception {
		this.databaseConfigConnector = databaseConfigConnector;
		connect();
	}

	public synchronized void connect() throws Exception {
		StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
				.append(databaseConfigConnector.mysqlHost).append(':');

		int mysqlUrlPort;
		SshTunnelConfig sshTunnelConfig = databaseConfigConnector.sshTunnelConfig;
		if (sshTunnelConfig != null) {

			jsch = new JSch();
			jsch.addIdentity(sshTunnelConfig.key);

			sshSession = jsch.getSession(sshTunnelConfig.user, sshTunnelConfig.host, sshTunnelConfig.port);

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			config.put("Compression", "yes");
			config.put("ConnectionAttempts", "2");
			sshSession.setConfig(config);

			sshSession.connect();

			int localPort = findAvailablePort();
			mysqlUrlPort = sshSession.setPortForwardingL(localPort, "127.0.0.1", databaseConfigConnector.mysqlPort);
		} else {
			mysqlUrlPort = databaseConfigConnector.mysqlPort;
		}
		jdbcUrl.append(mysqlUrlPort).append('/').append(databaseConfigConnector.mysqlDatabase)
				.append("?user=").append(databaseConfigConnector.mysqlUser)
				.append("&password=").append(databaseConfigConnector.mysqlPassword);

		String driverName = "com.mysql.jdbc.Driver";
		Class.forName(driverName).newInstance();

		pooledDataSource = new ComboPooledDataSource();
		pooledDataSource.setDriverClass(driverName);
		pooledDataSource.setJdbcUrl(jdbcUrl.toString());
		pooledDataSource.setMinPoolSize(1);
		pooledDataSource.setAcquireIncrement(1);
		pooledDataSource.setMaxPoolSize(20);
		pooledDataSource.setCheckoutTimeout((int) Duration.ofMinutes(1).toMillis());
		pooledDataSource.setTestConnectionOnCheckin(false);
		pooledDataSource.setTestConnectionOnCheckout(true);

		try(Connection connection = pooledDataSource.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try(ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
					ResultSetMetaData rsmd = resultSet.getMetaData();
					int columnsNumber = rsmd.getColumnCount();
					while (resultSet.next()) {
						String str = "";
						for (int i = 1; i <= columnsNumber; i++) {
							if (i > 1) str += ",  ";
							String columnValue = resultSet.getString(i);
							str += columnValue + " " + rsmd.getColumnName(i);
						}
						log.debug(str);
					}
				}
			}
		}
		log.debug("Connected by hgmd.");
	}

	public Connection createConnection() {
		try {
			return pooledDataSource.getConnection();
		} catch (SQLException e) {
			log.debug("Exception", e);
			throw ExceptionBuilder.buildOperationException(e);
		}
	}

	public synchronized void disconnect() {
		log.debug("Disconnect by clinVar.");
		if (pooledDataSource != null) {
			pooledDataSource.close();
			pooledDataSource = null;
		}
		if (sshSession != null) {
			sshSession.disconnect();
			sshSession = null;
		}
		jsch = null;
	}

	private synchronized void reconnect() throws Exception {
		disconnect();
		connect();
	}

	@Override
	public void close() throws IOException {
		disconnect();
	}

	private static int findAvailablePort() {
		for (int port = 3307; port < 4000; ++port) {
			if (isAvailablePort(port)) {
				return port;
			}
		}
		throw new RuntimeException("Not found available port");
	}

	private static boolean isAvailablePort(int port) {
		try (ServerSocket ss = new ServerSocket(port)) {
			ss.setReuseAddress(true);
		} catch (IOException ignored) {
			return false;
		}

		try (DatagramSocket ds = new DatagramSocket(port)) {
			ds.setReuseAddress(true);
		} catch (IOException ignored) {
			return false;
		}

		try (Socket s = new Socket ("localhost", port)) {
			return false;
		} catch (IOException ignored) {
			return true;
		}
	}
}
