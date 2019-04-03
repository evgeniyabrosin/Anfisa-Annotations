package org.forome.annotation.struct;

public class Sample {

    /**
     *                 sample["name"]      = sample_map.get(id, id)
     *                 sample['family']    = family
     *                 sample['id']        = id
     *                 sample['father']    = father
     *                 sample['mother']    = mother
     *                 sample['sex']       = int(sex)
     *                 sample['affected']  = (int(affected) == 2)
     */

    public final String id;
    public final String name;
    public final String family;
    public final String father;
    public final String mother;
    public final int sex;
    public final boolean affected;

    public Sample(String id, String name, String family, String father, String mother, int sex, boolean affected) {
        this.id = id;
        this.name = name;
        this.family = family;
        this.father = father;
        this.mother = mother;
        this.sex = sex;
        this.affected = affected;
    }
}
