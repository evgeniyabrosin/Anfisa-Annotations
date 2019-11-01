package org.forome.annotation.struct.mcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cohort cohort = (Cohort) o;
        return Objects.equals(name, cohort.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
