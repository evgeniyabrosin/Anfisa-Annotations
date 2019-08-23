package org.forome.annotation.connector.anfisa.struct;

public class Consequences
{
    public enum Severity
    {
        DAMAGING,
        MISSENSE,
        OTHER
    }

    public static Severity severity(String csq)
    {
        if (AnfisaVariant.CSQ_DAMAGING.contains (csq))
            return Severity.DAMAGING;
        if (AnfisaVariant.CSQ_MISSENSE.contains (csq))
            return Severity.MISSENSE;
        return Severity.OTHER;
    }
}
