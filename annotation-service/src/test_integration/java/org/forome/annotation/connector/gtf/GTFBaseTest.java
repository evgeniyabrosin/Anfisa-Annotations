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
//	protected GnomadConnector gnomadConnector;
//	protected HgmdConnector hgmdConnector;
//	protected ClinvarConnector clinvarConnector;
//	protected LiftoverConnector liftoverConnector;
//	protected AnfisaConnector anfisaConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
//		gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});
		gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
//		hgmdConnector = new HgmdConnector(serviceConfig.hgmdConfigConnector);
//		clinvarConnector = new ClinvarConnector(serviceConfig.clinVarConfigConnector);
//		liftoverConnector = new LiftoverConnector();
//		anfisaConnector = new AnfisaConnector(gnomadConnector, hgmdConnector, clinvarConnector, liftoverConnector);
	}
}
