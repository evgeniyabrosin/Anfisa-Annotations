package org.forome.annotation.iterator.cnv;

import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CNVFileIteratorTest {

    @Test
    public void test() throws URISyntaxException {
        Path pathCnv = Paths.get(getClass().getClassLoader().getResource("cnv/deletions.svaba.exons.txt").toURI());
        CNVFileIterator cnvFileIterator = new CNVFileIterator(pathCnv);

        VariantCNV variant1 = cnvFileIterator.next();
        Assert.assertNotNull(variant1);
        Assert.assertEquals(Chromosome.of("9"), variant1.chromosome);
        Assert.assertEquals(140772677, variant1.start);
        Assert.assertEquals(140777187, variant1.end);


        VariantCNV variant2 = cnvFileIterator.next();
        Assert.assertNotNull(variant2);
        Assert.assertEquals(Chromosome.of("9"), variant2.chromosome);
        Assert.assertEquals(140772688, variant2.start);
        Assert.assertEquals(140777198, variant2.end);


        VariantCNV variant3 = cnvFileIterator.next();
        Assert.assertNotNull(variant3);
        Assert.assertEquals(Chromosome.of("10"), variant3.chromosome);
        Assert.assertEquals(140772688, variant3.start);
        Assert.assertEquals(140777198, variant3.end);


        //Проверяем, что больше вариантов нет
        Assert.assertFalse(cnvFileIterator.hasNext());
    }
}


