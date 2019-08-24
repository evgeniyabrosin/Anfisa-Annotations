package org.forome.annotation;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.GnomadConnectorImpl;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepInlineService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnfisaBaseTest {

	private final static Logger log = LoggerFactory.getLogger(AnfisaBaseTest.class);

	private static SSHConnectService sshTunnelService;

	protected static GnomadConnector gnomadConnector;
	protected static SpliceAIConnector spliceAIConnector;
	protected static ConservationConnector conservationConnector;
	protected static HgmdConnector hgmdConnector;
	protected static ClinvarConnector clinvarConnector;
	protected static LiftoverConnector liftoverConnector;
	protected static GTFConnector gtfConnector;
	protected static EnsemblVepService ensemblVepService;
	protected static AnfisaConnector anfisaConnector;

	@BeforeClass
	public static void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		sshTunnelService = new SSHConnectService();
		gnomadConnector = new GnomadConnectorImpl(sshTunnelService, serviceConfig.gnomadConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		spliceAIConnector = new SpliceAIConnector(sshTunnelService, serviceConfig.spliceAIConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		conservationConnector = new ConservationConnector(sshTunnelService, serviceConfig.conservationConfigConnector);
		hgmdConnector = new HgmdConnector(sshTunnelService, serviceConfig.hgmdConfigConnector);
		clinvarConnector = new ClinvarConnector(sshTunnelService, serviceConfig.clinVarConfigConnector);
		liftoverConnector = new LiftoverConnector();
		gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		ensemblVepService = new EnsemblVepInlineService(sshTunnelService, serviceConfig.ensemblVepConfigConnector);
		anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
				conservationConnector,
				hgmdConnector,
				clinvarConnector,
				liftoverConnector,
				gtfConnector,
				(t, e) -> {
					log.error("Fail", e);
					Assert.fail();
				}
		);
	}

	@AfterClass
	public static void destroy() {
		anfisaConnector.close();
		gtfConnector.close();
		liftoverConnector.close();
		clinvarConnector.close();
		hgmdConnector.close();
		conservationConnector.close();
		spliceAIConnector.close();
		gnomadConnector.close();

		sshTunnelService.close();
	}
}
