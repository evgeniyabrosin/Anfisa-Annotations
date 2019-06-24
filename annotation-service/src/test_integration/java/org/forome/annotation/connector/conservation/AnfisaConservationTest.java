package org.forome.annotation.connector.conservation;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.struct.Chromosome;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnfisaConservationTest extends AnfisaBaseTest {

    private final static Logger log = LoggerFactory.getLogger(AnfisaConservationTest.class);

    @Test
    public void testSimple() {
        Conservation conservation1 = conservationConnector.getConservation(new Chromosome("1"), 11980267);
        assertDouble(0.032d, conservation1.priPhCons);
        assertDouble(0d, conservation1.mamPhCons);
        assertDouble(0d, conservation1.verPhCons);
        assertDouble(0.595d, conservation1.priPhyloP);
        assertDouble(-0.91d, conservation1.mamPhyloP);
        assertDouble(-0.574d, conservation1.verPhyloP);
        assertDouble(null, conservation1.gerpRS);
        Assert.assertEquals(null, conservation1.getGerpRSpval());
        assertDouble(11d, conservation1.gerpN);
        assertDouble(-22d, conservation1.gerpS);

        Conservation conservation2 = conservationConnector.getConservation(new Chromosome("14"), 16026153);
        assertDouble(0.117d, conservation2.priPhCons);
        assertDouble(0.117d, conservation2.mamPhCons);
        assertDouble(0.729d, conservation2.verPhCons);
        assertDouble(0.124d, conservation2.priPhyloP);
        assertDouble(0.125d, conservation2.mamPhyloP);
        assertDouble(-0.92d, conservation2.verPhyloP);
        assertDouble(331.536d, conservation2.gerpRS);
        Assert.assertEquals(Conservation.convFromL(-9.355021094467402d), conservation2.getGerpRSpval());
        assertDouble(3.59d, conservation2.gerpN);
        assertDouble(-5.72d, conservation2.gerpS);
    }

    @Test
    public void test() throws Exception {
        //chr1:16378047 G>T
        anfisaConnector.request("1", 16378047, 16378047, "T").get().forEach(anfisaResult -> {
            Conservation conservation = anfisaResult.view.bioinformatics.conservation;

        });

        //chr13:86369589 A>G
//        anfisaConnector.request("13", 86369589, 86369589, "G").get().forEach(anfisaResult -> {
//            Conservation conservation = anfisaResult.view.bioinformatics.conservation;
//
//        });
    }

    private void assertDouble(Double expected, Double actual) {
        if (expected != actual) {
            Assert.assertEquals((double)expected, (double)actual, 0.0000000000000001d);
        }
    }
}
