/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.inventory;

import org.forome.annotation.struct.Assembly;
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
		Assert.assertEquals(Assembly.GRCh38, inventory.assembly);
		Assert.assertEquals("bch0051.fam", inventory.famFile.getFileName().toString());
		Assert.assertEquals("bch0051.csv", inventory.patientIdsFile.getFileName().toString());
		Assert.assertEquals("bch0051.vcf", inventory.vcfFile.getFileName().toString());
		Assert.assertEquals("bch0051_vep.json", inventory.vepJsonFile.getFileName().toString());
		Assert.assertEquals("bch0051_anfisa.json.gz", inventory.outFile.getFileName().toString());
		Assert.assertEquals("anno.log", inventory.logFile.getFileName().toString());
	}
}
