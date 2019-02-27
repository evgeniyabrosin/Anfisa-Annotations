package org.forome.annotation.connector.gtf;

import org.junit.Test;

public class GTFSimpleTest extends GTFBaseTest {

	@Test
	public void test() throws Exception {
		gtfConnector.request("2", 73675228).get();
	}
}
