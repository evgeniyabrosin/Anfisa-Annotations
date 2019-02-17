package ru.processtech.forome.annotation.config.connector.database;

import net.minidev.json.JSONObject;
import ru.processtech.forome.annotation.config.sshtunnel.SshTunnelConfig;

public abstract class DatabaseConfigConnector {

	public final SshTunnelConfig sshTunnelConfig;

	public final String mysqlHost;
	public final int mysqlPort;
	public final String mysqlDatabase;
	public final String mysqlUser;
	public final String mysqlPassword;

	public DatabaseConfigConnector(JSONObject parse) {
		if (parse.containsKey("ssh_tunnel")) {
			sshTunnelConfig = new SshTunnelConfig((JSONObject) parse.get("ssh_tunnel"));
		} else {
			sshTunnelConfig = null;
		}

		JSONObject parseMysql = (JSONObject) parse.get("mysql");
		mysqlHost = parseMysql.getAsString("host");
		mysqlPort = parseMysql.getAsNumber("port").intValue();
		mysqlDatabase = parseMysql.getAsString("database");
		mysqlUser = parseMysql.getAsString("user");
		mysqlPassword = parseMysql.getAsString("password");
	}
}
