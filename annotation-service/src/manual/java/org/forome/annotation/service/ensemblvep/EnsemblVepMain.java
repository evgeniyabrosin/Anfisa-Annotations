package org.forome.annotation.service.ensemblvep;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.service.ssh.SSHConnectService;

public class EnsemblVepMain {

    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = new ServiceConfig();
        SSHConnectService sshTunnelService = new SSHConnectService();

        try(EnsemblVepService ensemblVepService = new EnsemblVepService(sshTunnelService, serviceConfig.ensemblVepConfigConnector)) {
            ensemblVepService.getVepJson(null, 0, 0, null);
        }
    }
}
