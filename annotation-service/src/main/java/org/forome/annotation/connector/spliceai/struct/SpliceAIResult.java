package org.forome.annotation.connector.spliceai.struct;

import java.util.Map;

public class SpliceAIResult {

    public static class DictSql {

        public final int dp_ag;
        public final int dp_al;
        public final int dp_dg;
        public final int dp_dl;
        public final float ds_ag;
        public final float ds_al;
        public final float ds_dg;
        public final float ds_dl;

        public DictSql(
                int dp_ag, int dp_al, int dp_dg, int dp_dl,
                float ds_ag, float ds_al, float ds_dg, float ds_dl
        ) {
            this.dp_ag = dp_ag;
            this.dp_al = dp_al;
            this.dp_dg = dp_dg;
            this.dp_dl = dp_dl;
            this.ds_ag = ds_ag;
            this.ds_al = ds_al;
            this.ds_dg = ds_dg;
            this.ds_dl = ds_dl;
        }

        public Number getValue(String key) {
            switch (key) {
                case "DP_AG":
                    return dp_ag;
                case "DP_AL":
                    return dp_al;
                case "DP_DG":
                    return dp_dg;
                case "DP_DL":
                    return dp_dl;
                case "DS_AG":
                    return ds_ag;
                case "DS_AL":
                    return ds_al;
                case "DS_DG":
                    return ds_dg;
                case "DS_DL":
                    return ds_dl;
                default:
                    throw new RuntimeException("Unknown key");
            }
        }
    }

    public final String cases;
    public final Float max_ds;

    public final Map<String, DictSql> dict_sql;

    public SpliceAIResult(String cases, Float max_ds, Map<String, DictSql> dict_sql) {
        this.cases = cases;
        this.max_ds = max_ds;
        this.dict_sql = dict_sql;
    }

}
