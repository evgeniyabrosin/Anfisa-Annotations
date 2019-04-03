package org.forome.annotation.config.sshtunnel;

import net.minidev.json.JSONObject;

public class SshTunnelConfig {

	public final String host;
	public final int port;
	public final String user;
	public final String key;

	public SshTunnelConfig(JSONObject parse) {
		host = parse.getAsString("host");
		port = parse.getAsNumber("port").intValue();
		user = parse.getAsString("user");
		key = parse.getAsString("key");
	}
}
