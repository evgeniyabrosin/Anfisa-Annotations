package org.forome.annotation.connector.conservation;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Для проверки необходимо использовать сайт
 * https://evs.gs.washington.edu/EVS/ - поле gerpRS
 */
public class ConservationTest extends AnfisaBaseTest {

    private final static Logger log = LoggerFactory.getLogger(ConservationTest.class);

    @Test
    public void testSimple() {
        //chr1:16378047 G>T rs35351345 4.48
        Chromosome chr1 = new Chromosome("1");
        Position<Long> pos1 = new Position<>(16378047L);
        Position<Integer> hgmdHG38_1 = liftoverConnector.toHG38(chr1, pos1);
        Conservation conservation1 = conservationConnector.getConservation(chr1, pos1, hgmdHG38_1, "G", "T");
        assertDouble(0.183d, conservation1.priPhCons);
        assertDouble(0.999d, conservation1.mamPhCons);
        assertDouble(1d, conservation1.verPhCons);
        assertDouble(0.587d, conservation1.priPhyloP);
        assertDouble(4.289d, conservation1.mamPhyloP);
        assertDouble(7.472d, conservation1.verPhyloP);
        assertDouble(4.48d, conservation1.gerpRS);//Проверяем на внешнем ресурсе
        Assert.assertEquals(null, conservation1.getGerpRSpval());
        assertDouble(4.48d, conservation1.gerpN);
        assertDouble(18.5d, conservation1.gerpS);

        //chr4:55593464 A>C rs3822214 1.82
        Chromosome chr2 = new Chromosome("4");
        Position<Long> pos2 = new Position<>(55593464L);
        Position<Integer> hgmdHG38_2 = liftoverConnector.toHG38(chr2, pos2);
        Conservation conservation2 = conservationConnector.getConservation(chr2, pos2, hgmdHG38_2, "A", "C");
        assertDouble(0.935d, conservation2.priPhCons);
        assertDouble(1d, conservation2.mamPhCons);
        assertDouble(0.778d, conservation2.verPhCons);
        assertDouble(0.475d, conservation2.priPhyloP);
        assertDouble(1.04d, conservation2.mamPhyloP);
        assertDouble(0.875d, conservation2.verPhyloP);
        assertDouble(1.82d, conservation2.gerpRS);//Проверяем на внешнем ресурсе
        Assert.assertEquals(null, conservation2.getGerpRSpval());
        assertDouble(5.9d, conservation2.gerpN);
        assertDouble(-36.6d, conservation2.gerpS);

        //chr9:139092481 G>C rs190916587 0.34
        Chromosome chr3 = new Chromosome("9");
        Position<Long> pos3 = new Position<>(139092481L);
        Position<Integer> hgmdHG38_3 = liftoverConnector.toHG38(chr3, pos3);
        Conservation conservation3 = conservationConnector.getConservation(chr3, pos3, hgmdHG38_3, "G", "C");
        assertDouble(0.99d, conservation3.priPhCons);
        assertDouble(0.993d, conservation3.mamPhCons);
        assertDouble(0.038d, conservation3.verPhCons);
        assertDouble(0.595d, conservation3.priPhyloP);
        assertDouble(-0.01d, conservation3.mamPhyloP);
        assertDouble(-0.417d, conservation3.verPhyloP);
        assertDouble(0.343d, conservation3.gerpRS);//Проверяем на внешнем ресурсе
        Assert.assertEquals(Conservation.convFromL(-232.99099978037597d), conservation3.getGerpRSpval());
        assertDouble(4.65d, conservation3.gerpN);
        assertDouble(-8.57d, conservation3.gerpS);

        //chr13:86369589 A>G rs199516562 5.86
        Chromosome chr4 = new Chromosome("13");
        Position<Long> pos4 = new Position<>(86369589L);
        Position<Integer> hgmdHG38_4 = liftoverConnector.toHG38(chr4, pos4);
        Conservation conservation4 = conservationConnector.getConservation(chr4, pos4, hgmdHG38_4, "A", "G");
        assertDouble(0.617d, conservation4.priPhCons);
        assertDouble(1d, conservation4.mamPhCons);
        assertDouble(1d, conservation4.verPhCons);
        assertDouble(0.475d, conservation4.priPhyloP);
        assertDouble(3.37d, conservation4.mamPhyloP);
        assertDouble(8.687d, conservation4.verPhyloP);
        assertDouble(5.86d, conservation4.gerpRS);//Проверяем на внешнем ресурсе
        Assert.assertEquals(Conservation.convFromL(-10000d), conservation4.getGerpRSpval());
        assertDouble(5.86d, conservation4.gerpN);
        assertDouble(19.8d, conservation4.gerpS);

        //chr2:21266774 GGCAGCGCCA>G rs17240441
        Chromosome chr5 = new Chromosome("2");
        Position<Long> pos5 = new Position<>(21266774L);
        Position<Integer> hgmdHG38_5 = liftoverConnector.toHG38(chr5, pos5);
        Conservation conservation5 = conservationConnector.getConservation(chr5, pos5, hgmdHG38_5, "GGCAGCGCCA", "G");
        Assert.assertEquals(null, conservation5);//Вычисляются только однобуквенные и инсерции

        //chr1:175949371 A>AACC
        Chromosome chr7 = new Chromosome("1");
        Position<Long> pos7 = new Position<>(175949371L);
        Position<Integer> hgmdHG38_7 = liftoverConnector.toHG38(chr7, pos7);
        Conservation conservation7 = conservationConnector.getConservation(chr7, pos7, hgmdHG38_7, "A", "AACC");
        assertDouble(2.37d, conservation7.gerpRS);//Проверяем на внешнем ресурсе
    }

    @Test
    public void testMulti() {
        //chr2:55863360 T>TA rs35916020
        Chromosome chr1 = new Chromosome("2");
        Position<Long> pos1 = new Position<>(55863361L, 55863360L);
        Position<Integer> hgmdHG38_1 = liftoverConnector.toHG38(chr1, pos1);
        Conservation conservation1 = conservationConnector.getConservation(chr1, pos1, hgmdHG38_1, "T", "TA");
        assertDouble(-0.868d, conservation1.gerpRS);//Проверяем на внешнем ресурсе

        //chr1:120681278 A>AT  rs201956187
        //Проверяем ситуацию когда координаты hg38 - вычислить не удалось
        Chromosome chr2 = new Chromosome("1");
        Position<Long> pos2 = new Position<>(120681279L, 120681278L);
        Position<Integer> hgmdHG38_2 = liftoverConnector.toHG38(chr2, pos2);
        Conservation conservation2 = conservationConnector.getConservation(chr2, pos2, hgmdHG38_2, "A", "AT");
        Assert.assertEquals(null, conservation2.priPhCons);
        Assert.assertEquals(null, conservation2.mamPhCons);
        Assert.assertEquals(null, conservation2.verPhCons);
        Assert.assertEquals(null, conservation2.priPhyloP);
        Assert.assertEquals(null, conservation2.mamPhyloP);
        Assert.assertEquals(null, conservation2.verPhyloP);
        assertDouble(0.0d, conservation2.gerpRS);//TODO необходимо подтверждение на внешнем ресурсе
        Assert.assertEquals(null, conservation2.getGerpRSpval());
        assertDouble(0.0d, conservation2.gerpN);
        Assert.assertEquals(null, conservation2.gerpS);
    }

    private void assertDouble(Double expected, Double actual) {
        if (expected != actual) {
            Assert.assertEquals((double) expected, (double) actual, 0.0000000000000001d);
        }
    }
}
