package org.forome.annotation.connector.liftover;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.junit.Assert;
import org.junit.Test;

public class LiftoverTest extends AnfisaBaseTest {

    @Test
    public void testFail() {
        //chr1:120681278 A>AT  rs201956187
        /**
         * Это случай, когда программа liftOver работает не правильно. Причем ее веб версия дает ошибку
         * (https://genome.ucsc.edu/cgi-bin/hgLiftOver дает #Split in new
         * chr1:120681278-120681279 - Sequence insufficiently intersects multiple chains),
         * а утилита просто молча выдает ахинею. Причина понятна, как раз в этом месте принципиально поменялся способ,
         * которым собирается референс. Как это отлавливать не знаю.
         * Сам dbSNP показывает этот вариант в 38-й сборке, как chr1:120138709-120138721, что тоже странно,
         * поскольку он больше двух нуклеотидов не занимает (https://www.ncbi.nlm.nih.gov/snp/rs201956187),
         * отуда берется 12 позиций я не понимаю.
         * ==================================
         * Если длина мутации первышает 10 позиций (base pairs), то мы такие мутации переводить в 38-ю сборку не будем
         */
        Assert.assertEquals(null, liftoverConnector.toHG38(
                new Chromosome("1"), new Position<Long>(120681279L, 120681278L)
        ));

    }
}
