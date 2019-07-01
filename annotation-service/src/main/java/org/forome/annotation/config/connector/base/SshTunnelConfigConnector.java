package org.forome.annotation.config.connector.base;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;

public abstract class SshTunnelConfigConnector {

    public final SshTunnelConfig sshTunnelConfig;

    public SshTunnelConfigConnector(JSONObject parse) {
        if (parse.containsKey("ssh_tunnel")) {
            sshTunnelConfig = new SshTunnelConfig((JSONObject) parse.get("ssh_tunnel"));
        } else {
            sshTunnelConfig = null;
        }
    }
}
