package org.forome.annotation.struct.variant;

public enum VariantType {

    SNV("SNV"),

    INDEL("indel"),

    SEQUENCE_ALTERATION("sequence_alteration"),

    /** Deletion relative to the reference */
    DEL("deletion"),

    /** Insertion of novel sequence relative to the reference */
    INS("insertion"),

    /** Copy number variable region */
    CNV("CNV: deletion");

    private final String jsonValue;

    VariantType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String toJSON(){
        return jsonValue;
    }

    public static VariantType findByName(String value) {
        for (VariantType item: VariantType.values()) {
            if (item.toJSON().equals(value)) {
                return item;
            }
        }
        throw new RuntimeException("Unknown type: " + value);
    }
}
