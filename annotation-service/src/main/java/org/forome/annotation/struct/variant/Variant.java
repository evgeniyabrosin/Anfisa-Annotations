package org.forome.annotation.struct.variant;

import org.forome.annotation.struct.Chromosome;

public class Variant {

    public final Chromosome chromosome;
    public final int start;
    public final int end;

    public Variant(Chromosome chromosome, int start, int end) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
    }
}
