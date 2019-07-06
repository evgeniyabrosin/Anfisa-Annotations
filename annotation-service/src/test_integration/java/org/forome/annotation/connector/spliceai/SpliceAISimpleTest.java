package org.forome.annotation.connector.spliceai;

import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class SpliceAISimpleTest extends SpliceAIBaseTest {

    @Test
    public void testGetAll() throws Exception {
        SpliceAIResult spliceAIResult = spliceAIConnector.getAll("10", 92897L, "A", new ArrayList<String>(){{ add("C"); }});
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

    @Test
    public void testDataVersion() throws Exception {
        Assert.assertEquals("GRCh37/hg19", spliceAIConnector.getSpliceAIDataVersion());
    }

}
