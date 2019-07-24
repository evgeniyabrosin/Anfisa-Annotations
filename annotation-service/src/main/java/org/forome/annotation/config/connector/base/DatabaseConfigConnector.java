package org.forome.annotation.config.connector.base;

import net.minidev.json.JSONObject;

public abstract class DatabaseConfigConnector extends SshTunnelConfigConnector {

	public final String mysqlHost;
	public final int mysqlPort;
	public final String mysqlDatabase;
	public final String mysqlUser;
	public final String mysqlPassword;

	public DatabaseConfigConnector(JSONObject parse) {
		super(parse);

		JSONObject parseMysql = (JSONObject) parse.get("mysql");
		mysqlHost = parseMysql.getAsString("host");
		mysqlPort = parseMysql.getAsNumber("port").intValue();
		mysqlDatabase = parseMysql.getAsString("database");
		mysqlUser = parseMysql.getAsString("user");
		mysqlPassword = parseMysql.getAsString("password");
	}
}
