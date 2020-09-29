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

package org.forome.annotation.data.spliceai;

import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.struct.Allele;
import org.forome.core.struct.Assembly;
import org.junit.Assert;
import org.junit.Test;

public class SpliceAISimpleTest extends SpliceAIBaseTest {

	@Test
	public void testGetAll() throws Exception {
		SpliceAIResult spliceAIResult = spliceAIConnector.getAll(null, Assembly.GRCh37,"10", 92897, "A", new Allele("C"));
		Assert.assertEquals("unlikely", spliceAIResult.cases);
		Assert.assertEquals(0.1391f, spliceAIResult.max_ds, 0.000001f);

		Assert.assertEquals(1, spliceAIResult.dict_sql.size());
		Assert.assertEquals("C/TUBB8/-/E", spliceAIResult.dict_sql.keySet().iterator().next());
		SpliceAIResult.DictSql dictSql = spliceAIResult.dict_sql.values().iterator().next();
		Assert.assertEquals(-1, dictSql.dp_ag);
		Assert.assertEquals(1, dictSql.dp_al);
		Assert.assertEquals(-1, dictSql.dp_dg);
		Assert.assertEquals(28, dictSql.dp_dl);
		Assert.assertEquals(0.1391f, dictSql.ds_ag, 0.000001f);
		Assert.assertEquals(0.0f, dictSql.ds_al, 0.000001f);
		Assert.assertEquals(0.0f, dictSql.ds_dg, 0.000001f);
		Assert.assertEquals(0.0f, dictSql.ds_dl, 0.000001f);
	}

}
