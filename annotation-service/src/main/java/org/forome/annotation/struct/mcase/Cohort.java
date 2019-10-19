package org.forome.annotation.struct.mcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cohort {

    public final String name;

    private List<Sample> samples;

    public Cohort(String name) {
        this.name = name;
        this.samples = new ArrayList<>();
    }

    protected void register(Sample sample) {
        samples.add(sample);
    }

    public List<Sample> getSamples() {
        return Collections.unmodifiableList(samples);
    }
}
