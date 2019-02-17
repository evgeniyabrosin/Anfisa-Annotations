package ru.processtech.forome.annotation.connector.gnomad;

import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.processtech.forome.annotation.config.ServiceConfig;

public class GnomadBaseTest {

	private final static Logger log = LoggerFactory.getLogger(GnomadBaseTest.class);

	protected GnomadConnector gnomadConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
	}
}
