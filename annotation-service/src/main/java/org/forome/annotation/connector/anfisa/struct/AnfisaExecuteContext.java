package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.variant.Variant;

public class AnfisaExecuteContext {

    public final AnfisaInput anfisaInput;

    public final Variant variant;
    public final JSONObject vepJson;

    public Double gnomadAfFam;

    public AnfisaExecuteContext(
            AnfisaInput anfisaInput,
            Variant variant,
            JSONObject vepJson
    ) {
        this.anfisaInput = anfisaInput;

        this.variant = variant;
        this.vepJson = vepJson;
    }

}
