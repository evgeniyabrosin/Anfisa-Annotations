package org.forome.annotation.connector.ensemblvep;

import org.forome.annotation.config.ServiceConfig;

public class EnsemblVepMain {

    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = new ServiceConfig();
        EnsemblVepConnector ensemblVepConnector = new EnsemblVepConnector(serviceConfig.ensemblVepConfigConnector);
        ensemblVepConnector.getVepJson(null, 0, 0, null);
        ensemblVepConnector.close();
    }
}
