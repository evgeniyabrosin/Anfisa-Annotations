package org.forome.annotation.network.transport.builder;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.forome.annotation.network.exception.NetworkException;

public class BuilderHttpConnector {

	protected int port;
	private String host;

	public BuilderHttpConnector(int port) {
		this.port = port;
		this.host = null;
	}

	public Connector build(Server server) throws NetworkException {
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setHost(host);
		return connector;
	}
}
