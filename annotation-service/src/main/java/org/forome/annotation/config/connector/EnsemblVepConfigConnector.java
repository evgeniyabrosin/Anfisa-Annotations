package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.base.SshTunnelConfigConnector;

public class EnsemblVepConfigConnector extends SshTunnelConfigConnector {

    public final String cmd;

    public EnsemblVepConfigConnector(JSONObject parse) {
        super(parse);

        JSONObject parseApp = (JSONObject) parse.get("app");
        cmd = parseApp.getAsString("cmd");
    }
}
