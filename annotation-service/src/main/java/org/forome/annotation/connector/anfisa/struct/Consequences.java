package org.forome.annotation.connector.anfisa.struct;

import org.forome.annotation.struct.Variant;

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
        if (Variant.CSQ_DAMAGING.contains (csq))
            return Severity.DAMAGING;
        if (Variant.CSQ_MISSENSE.contains (csq))
            return Severity.MISSENSE;
        return Severity.OTHER;
    }
}
