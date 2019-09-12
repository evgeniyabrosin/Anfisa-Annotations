package org.forome.annotation.service.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.forome.annotation.config.connector.base.DatabaseConfigConnector;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnectService implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(DatabaseConnectService.class);

    private final SSHConnectService sshTunnelService;

    private final Map<String, ComboPooledDataSource> dataSources;

    public DatabaseConnectService(SSHConnectService sshTunnelService) {
        this.sshTunnelService = sshTunnelService;
        this.dataSources = new HashMap<>();
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
        if (databaseConfigConnector.sshTunnelConfig!=null) {
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
}
