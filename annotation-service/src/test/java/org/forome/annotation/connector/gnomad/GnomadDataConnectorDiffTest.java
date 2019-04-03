package org.forome.annotation.connector.gnomad;

import org.junit.Assert;
import org.junit.Test;

public class GnomadDataConnectorDiffTest {

	@Test
	public void test() throws Exception {
		Assert.assertEquals("-CATCATCATCAT", GnomadDataConnector.diff("CCATCATCATCAT", "C"));
		Assert.assertEquals("-CAT", GnomadDataConnector.diff("CCATCAT", "CCAT"));
		Assert.assertEquals("-CATCATCAT", GnomadDataConnector.diff("CCATCATCATCAT", "CCAT"));
		Assert.assertEquals("-CAT", GnomadDataConnector.diff("CCATCAT", "CCAT"));
		Assert.assertEquals("-CATCAT", GnomadDataConnector.diff("CCATCATCATCAT", "CCATCAT"));
		Assert.assertEquals("CAT", GnomadDataConnector.diff("CCATCATCATCAT", "CCATCATCATCATCAT"));
	}
}
