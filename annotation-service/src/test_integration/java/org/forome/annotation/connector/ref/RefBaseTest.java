package org.forome.annotation.connector.ref;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefBaseTest {

	private final static Logger log = LoggerFactory.getLogger(RefBaseTest.class);

	protected RefConnector refConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		refConnector = new RefConnector(sshTunnelService, serviceConfig.refConfigConnector);
	}
}
