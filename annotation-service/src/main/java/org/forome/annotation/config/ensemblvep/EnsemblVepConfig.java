package org.forome.annotation.config.ensemblvep;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.base.SshTunnelConfigConnector;

public class EnsemblVepConfig extends SshTunnelConfigConnector {

    public final String cmd;

    public EnsemblVepConfig(JSONObject parse) {
        super(parse);

        JSONObject parseApp = (JSONObject) parse.get("app");
        cmd = parseApp.getAsString("cmd");
    }
}
