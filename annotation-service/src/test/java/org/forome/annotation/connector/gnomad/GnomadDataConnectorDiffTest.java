package org.forome.annotation.connector.gnomad;

import org.forome.annotation.connector.gnomad.old.GnomadDataConnectorOld;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnomadDataConnectorDiffTest {

    private final static Logger log = LoggerFactory.getLogger(GnomadDataConnectorDiffTest.class);

    @Test
    public void test() throws Exception {
        Assert.assertEquals("-CATCATCATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "C"));
        Assert.assertEquals("-CAT", GnomadDataConnectorOld.diff("CCATCAT", "CCAT"));
        Assert.assertEquals("-CATCATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCAT"));
        Assert.assertEquals("-CAT", GnomadDataConnectorOld.diff("CCATCAT", "CCAT"));
        Assert.assertEquals("-CATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCATCAT"));
        Assert.assertEquals("CAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCATCATCATCATCAT"));
    }

}
