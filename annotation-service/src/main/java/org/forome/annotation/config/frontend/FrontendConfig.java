package org.forome.annotation.config.frontend;

import net.minidev.json.JSONObject;

public class FrontendConfig {

    public final String apikey;

    public FrontendConfig(JSONObject parse) {
        this.apikey = parse.getAsString("apikey");
    }
}
