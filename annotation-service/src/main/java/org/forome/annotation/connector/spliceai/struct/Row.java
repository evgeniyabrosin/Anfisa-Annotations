package org.forome.annotation.connector.spliceai.struct;

public class Row {

    public final String chrom;
    public final int pos;
    public final String ref;
    public final String alt;
    public final String symbol;
    public final String strand;
    public final String type;
    public final int dp_ag;
    public final int dp_al;
    public final int dp_dg;
    public final int dp_dl;
    public final float ds_ag;
    public final float ds_al;
    public final float ds_dg;
    public final float ds_dl;
    public final String id;
    public final float max_ds;

    public Row(
            String chrom, int pos, String ref, String alt,
            String symbol, String strand, String type,
            int dp_ag, int dp_al, int dp_dg, int dp_dl,
            float ds_ag, float ds_al, float ds_dg, float ds_dl,
            String id, float max_ds
    ) {
        this.chrom = chrom;
        this.pos = pos;
        this.ref = ref;
        this.alt = alt;
        this.symbol = symbol;
        this.strand = strand;
        this.type = type;
        this.dp_ag = dp_ag;
        this.dp_al = dp_al;
        this.dp_dg = dp_dg;
        this.dp_dl = dp_dl;
        this.ds_ag = ds_ag;
        this.ds_al = ds_al;
        this.ds_dg = ds_dg;
        this.ds_dl = ds_dl;
        this.id = id;
        this.max_ds = max_ds;
    }
}
