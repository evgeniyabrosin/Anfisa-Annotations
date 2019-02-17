package ru.processtech.forome.annotation.connector.gnomad;

import org.junit.Test;

public class GnomadSimpleTest extends GnomadBaseTest {

	@Test
	public void test() throws Exception {
//		gnomadConnector.request("1", 103471457, "CCATCAT", "CCAT");
//		gnomadConnector.request("1", 6484880, "A", "G");
//		gnomadConnector.request("4", 88536520, "T", "C");
		gnomadConnector.request("1", 24646091, "A", "G").get();
	}
}
