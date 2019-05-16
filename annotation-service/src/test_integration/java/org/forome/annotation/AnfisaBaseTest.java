package org.forome.annotation;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnfisaBaseTest {

	private final static Logger log = LoggerFactory.getLogger(AnfisaBaseTest.class);

	protected GnomadConnector gnomadConnector;
	protected SpliceAIConnector spliceAIConnector;
	protected HgmdConnector hgmdConnector;
	protected ClinvarConnector clinvarConnector;
	protected LiftoverConnector liftoverConnector;
	protected GTFConnector gtfConnector;
	protected AnfisaConnector anfisaConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		spliceAIConnector = new SpliceAIConnector(serviceConfig.spliceAIConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		hgmdConnector = new HgmdConnector(serviceConfig.hgmdConfigConnector);
		clinvarConnector = new ClinvarConnector(serviceConfig.clinVarConfigConnector);
		liftoverConnector = new LiftoverConnector();
		gtfConnector = new GTFConnector(serviceConfig.gtfConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
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

	@After
	public void destroy() {
		anfisaConnector.close();
	}
}
