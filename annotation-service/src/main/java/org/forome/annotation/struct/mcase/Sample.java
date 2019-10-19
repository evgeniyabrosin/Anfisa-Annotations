package org.forome.annotation.struct.mcase;

public class Sample {

    public final String id;
    public final String name;
    public final String family;
    public final String father;
    public final String mother;
    public final int sex;
    public final boolean affected;

    public final Cohort cohort;

    public Sample(String id, String name, String family, String father, String mother, int sex, boolean affected, Cohort cohort) {
        this.id = id;
        this.name = name;
        this.family = family;
        this.father = father;
        this.mother = mother;
        this.sex = sex;
        this.affected = affected;

        this.cohort = cohort;
        if (cohort != null) {
            cohort.register(this);
        }
    }
}
