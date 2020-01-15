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

package org.forome.annotation.data.gtex;

import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.data.gtex.struct.Tissue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class GTEXSimpleTest extends AnfisaBaseTest {

	@Test
	public void test1() {
		List<Tissue> tissues = anfisaConnector.getTissues(Collections.singletonList("UGT2B28"));
		Assert.assertEquals(3, tissues.size());

		Assert.assertEquals("Bladder", tissues.get(0).name);
		Assert.assertEquals(0.18f, tissues.get(0).expression, 0.009f);
		Assert.assertEquals("Bladder: 0.18 TPM", tissues.get(0).toJSON());

		Assert.assertEquals("Minor Salivary Gland", tissues.get(1).name);
		Assert.assertEquals(0.13f, tissues.get(1).expression, 0.009f);
		Assert.assertEquals("Minor Salivary Gland: 0.13 TPM", tissues.get(1).toJSON());

		Assert.assertEquals("Liver", tissues.get(2).name);
		Assert.assertEquals(0.05f, tissues.get(2).expression, 0.009f);
		Assert.assertEquals("Liver: 0.05 TPM", tissues.get(2).toJSON());
	}

	@Test
	public void test2() {
		List<Tissue> tissues = anfisaConnector.getTissues(Collections.singletonList("MUC4"));
		Assert.assertEquals(3, tissues.size());

		Assert.assertEquals("Colon - Transverse", tissues.get(0).name);
		Assert.assertEquals(24.7f, tissues.get(0).expression, 0.009f);
		Assert.assertEquals("Colon - Transverse: 24.7 TPM", tissues.get(0).toJSON());

		Assert.assertEquals("Esophagus - Mucosa", tissues.get(1).name);
		Assert.assertEquals(9.20f, tissues.get(1).expression, 0.009f);
		Assert.assertEquals("Esophagus - Mucosa: 9.20 TPM", tissues.get(1).toJSON());

		Assert.assertEquals("Minor Salivary Gland", tissues.get(2).name);
		Assert.assertEquals(6.57f, tissues.get(2).expression, 0.009f);
		Assert.assertEquals("Minor Salivary Gland: 6.57 TPM", tissues.get(2).toJSON());
	}

	@Test
	public void test3() {
		List<Tissue> tissues = anfisaConnector.getTissues(Collections.singletonList("PHF20"));
		Assert.assertEquals(5, tissues.size());

		Assert.assertEquals("Testis", tissues.get(0).name);
		Assert.assertEquals(27.8f, tissues.get(0).expression, 0.1f);
		Assert.assertEquals("Testis: 27.8 TPM", tissues.get(0).toJSON());

		Assert.assertEquals("Brain - Cerebellar Hemisphere", tissues.get(1).name);
		Assert.assertEquals(24.6f, tissues.get(1).expression, 0.1f);
		Assert.assertEquals("Brain - Cerebellar Hemisphere: 24.6 TPM", tissues.get(1).toJSON());

		Assert.assertEquals("Artery - Tibial", tissues.get(2).name);
		Assert.assertEquals(24.3f, tissues.get(2).expression, 0.1f);
		Assert.assertEquals("Artery - Tibial: 24.3 TPM", tissues.get(2).toJSON());

		Assert.assertEquals("Cells - EBV-transformed lymphocytes", tissues.get(3).name);
		Assert.assertEquals(24.2f, tissues.get(3).expression, 0.1f);
		Assert.assertEquals("Cells - EBV-transformed lymphocytes: 24.2 TPM", tissues.get(3).toJSON());

		Assert.assertEquals("Cells - Cultured fibroblasts", tissues.get(4).name);
		Assert.assertEquals(23.5f, tissues.get(4).expression, 0.1f);
		Assert.assertEquals("Cells - Cultured fibroblasts: 23.5 TPM", tissues.get(4).toJSON());
	}
}
