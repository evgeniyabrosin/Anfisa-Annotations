package org.forome.annotation.connector.gtf;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTFBaseTest {

	private final static Logger log = LoggerFactory.getLogger(GTFBaseTest.class);

	protected GTFConnector gtfConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
	}
}
