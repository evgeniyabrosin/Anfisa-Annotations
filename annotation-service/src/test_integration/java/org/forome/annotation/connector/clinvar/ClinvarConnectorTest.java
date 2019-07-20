package org.forome.annotation.connector.clinvar;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.connector.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.struct.Chromosome;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClinvarConnectorTest extends AnfisaBaseTest {

    private final static Logger log = LoggerFactory.getLogger(ClinvarConnectorTest.class);

    @Test
    public void testSimple() {
        //[APOB] chr2:21232803 T>C
        ClinvarVariantSummary clinvarVariantSummary = clinvarConnector.getDataVariantSummary(
                new Chromosome("2"), 21232803, 21232803
        );
        log.debug("");
    }

}
