package org.forome.annotation.connector.gnomad;

import org.forome.annotation.connector.gnomad.struct.GnomadResult;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnomadSimpleTest extends GnomadBaseTest {

	private final static Logger log = LoggerFactory.getLogger(GnomadSimpleTest.class);

	@Test
	public void test() throws Exception {
//		gnomadConnector.request("1", 103471457, "CCATCAT", "CCAT");
//		gnomadConnector.request("1", 6484880, "A", "G");
//		gnomadConnector.request("4", 88536520, "T", "C");
//		gnomadConnector.request("1", 24646091, "A", "G").get();
		GnomadResult gnomadResult = gnomadConnector.request("1", 6505823, "CACCA", "C").get();
		log.debug("gnomadResult: " + gnomadResult);
	}
}
