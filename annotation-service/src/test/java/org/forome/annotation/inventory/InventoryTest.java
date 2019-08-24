package org.forome.annotation.inventory;

import org.forome.annotation.struct.CasePlatform;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InventoryTest {

    @Test
    public void test() throws URISyntaxException {
        Path inventoryFile = Paths.get(getClass().getClassLoader().getResource("inventory/bch0051/bch0051_wgs_1.cfg").toURI());

        Inventory inventory = new Inventory.Builder(inventoryFile).build();
        Assert.assertEquals("bch0051", inventory.caseName);
        Assert.assertEquals(CasePlatform.WGS, inventory.casePlatform);
        Assert.assertEquals("bch0051.fam", inventory.famFile.getFileName().toString());
        Assert.assertEquals("bch0051.csv", inventory.patientIdsFile.getFileName().toString());
        Assert.assertEquals("bch0051.vcf", inventory.vcfFile.getFileName().toString());
        Assert.assertEquals("bch0051_vep.json", inventory.vepJsonFile.getFileName().toString());
        Assert.assertEquals("bch0051_anfisa.json.gz", inventory.outFile.getFileName().toString());
        Assert.assertEquals("anno.log", inventory.logFile.getFileName().toString());
    }
}
