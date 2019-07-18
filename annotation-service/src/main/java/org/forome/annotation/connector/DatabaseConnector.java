package org.forome.annotation.connector;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.time.Duration;

public class DatabaseConnector implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(DatabaseConnector.class);

    private final SSHConnectService sshTunnelService;

    private final DatabaseConfigConnector databaseConfigConnector;

    private ComboPooledDataSource pooledDataSource;

    public DatabaseConnector(SSHConnectService sshTunnelService, DatabaseConfigConnector databaseConfigConnector) throws Exception {
        this.sshTunnelService = sshTunnelService;
        this.databaseConfigConnector = databaseConfigConnector;
        connect();
    }

    public synchronized void connect() throws Exception {
        StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
                .append(databaseConfigConnector.mysqlHost).append(':');

        int mysqlUrlPort;
        SshTunnelConfig sshTunnelConfig = databaseConfigConnector.sshTunnelConfig;
        if (sshTunnelConfig != null) {
            SSHConnect sshTunnel = sshTunnelService.getSSHTunnel(
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

        pooledDataSource = new ComboPooledDataSource();
        pooledDataSource.setDriverClass(driverName);
        pooledDataSource.setJdbcUrl(jdbcUrl.toString());
        pooledDataSource.setMinPoolSize(1);
        pooledDataSource.setAcquireIncrement(1);
        pooledDataSource.setMaxPoolSize(20);
        pooledDataSource.setCheckoutTimeout((int) Duration.ofMinutes(1).toMillis());
        pooledDataSource.setTestConnectionOnCheckin(false);
        pooledDataSource.setTestConnectionOnCheckout(true);

        try (Connection connection = pooledDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
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
    }

    private synchronized void reconnect() throws Exception {
        disconnect();
        connect();
    }

    @Override
    public void close() {
        disconnect();
    }
}
