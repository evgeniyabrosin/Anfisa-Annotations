package org.forome.annotation;

import org.forome.annotation.config.Config;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.database.DatabaseService;
import org.forome.annotation.database.entityobject.user.UserReadable;
import org.forome.annotation.exception.ServiceException;
import org.forome.annotation.executionqueue.*;
import org.forome.annotation.network.NetworkService;
import org.forome.annotation.network.component.UserEditableComponent;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.utils.ArgumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
//vulitin@ip-172-31-24-96:~$ PYTHONPATH=/data/bgm/versions/master/anfisa python -m annotations.singleton -a gnomad 1:103471457 "CCATCAT>CCAT"
//		Namespace(annotations='gnomad', input=['1:103471457', 'CCATCAT>CCAT'], test=1)
//		{
//		"exomes": {
//		"AC": 154564,
//		"AF": 0.8046520344841948,
//		"AN": 192088
//		},
//		"genomes": {
//		"AC": 23164,
//		"AF": 0.7641353829913571,
//		"AN": 30314
//		},
//		"overall": {
//		"AC": 177728,
//		"AF": 0.7991295042310771,
//		"AN": 222402
//		},
//		"popmax": "EAS",
//		"popmax_af": 0.9925577156743621,
//		"popmax_an": 13168,
//		"url": [
//		"http://gnomad.broadinstitute.org/variant/1-103471456-CCAT-C"
//		]
//		}
//		0.00427389144897
//


public class Service {

    private final static Logger log = LoggerFactory.getLogger(Service.class);

    private static Service instance = null;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final ExecutionQueue executionQueue;

    private final Config config;
    private final ServiceConfig serviceConfig;
    private final SSHConnectService sshTunnelService;
    private final DatabaseService databaseService;
    private final NetworkService networkService;

    private final GnomadConnector gnomadConnector;
    private final SpliceAIConnector spliceAIConnector;
    private final ConservationConnector conservationConnector;
    private final HgmdConnector hgmdConnector;
    private final ClinvarConnector clinvarConnector;
    private final LiftoverConnector liftoverConnector;
    private final GTFConnector gtfConnector;
    private final AnfisaConnector anfisaConnector;

    private final NotificationService notificationService;

    public Service(ArgumentParser arguments, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) throws Exception {
        instance = this;

        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.executionQueue = new ExecutionQueue(uncaughtExceptionHandler);

        this.config = new Config();
        this.serviceConfig = new ServiceConfig();
        this.sshTunnelService = new SSHConnectService();
        this.databaseService = new DatabaseService(this);
        this.networkService = new NetworkService(arguments.port, uncaughtExceptionHandler);

        if (serviceConfig.notificationSlackConfig != null) {
            this.notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
        } else {
            this.notificationService = null;
        }

        this.gnomadConnector = new GnomadConnector(sshTunnelService, serviceConfig.gnomadConfigConnector, uncaughtExceptionHandler);
        this.spliceAIConnector = new SpliceAIConnector(sshTunnelService, serviceConfig.spliceAIConfigConnector, uncaughtExceptionHandler);
        this.conservationConnector = new ConservationConnector(sshTunnelService, serviceConfig.conservationConfigConnector);
        this.hgmdConnector = new HgmdConnector(sshTunnelService, serviceConfig.hgmdConfigConnector);
        this.clinvarConnector = new ClinvarConnector(sshTunnelService, serviceConfig.clinVarConfigConnector);
        this.liftoverConnector = new LiftoverConnector();
        this.gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, uncaughtExceptionHandler);
        this.anfisaConnector = new AnfisaConnector(
                gnomadConnector,
                spliceAIConnector,
                conservationConnector,
                hgmdConnector,
                clinvarConnector,
                liftoverConnector,
                gtfConnector,
                uncaughtExceptionHandler
        );

        executionQueue.execute(this, new Execution<Void>() {

            private ReadableResource<UserReadable> userReadableResource;
            private UserEditableComponent userEditableComponent;

            @Override
            public void prepare(ResourceProvider resources) {
                userReadableResource = resources.getReadableResource(UserReadable.class);
                userEditableComponent = new UserEditableComponent(resources);
            }

            @Override
            public Void execute(ExecutionTransaction transaction) throws ServiceException {
                if (!userReadableResource.iterator(transaction).hasNext()) {
                    userEditableComponent.create("admin", "b82nfGl5sdg", transaction);
                }
                return null;
            }
        }).get();
    }

    public ExecutionQueue getExecutionQueue() {
        return executionQueue;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    public Config getConfig() {
        return config;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public NetworkService getNetworkService() {
        return networkService;
    }

    public GnomadConnector getGnomadConnector() {
        return gnomadConnector;
    }

    public LiftoverConnector getLiftoverConnector() {
        return liftoverConnector;
    }

    public GTFConnector getGtfConnector() {
        return gtfConnector;
    }

    public AnfisaConnector getAnfisaConnector() {
        return anfisaConnector;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void stop() {
        anfisaConnector.close();
        gtfConnector.close();
        liftoverConnector.close();
        clinvarConnector.close();
        hgmdConnector.close();
        conservationConnector.close();
        spliceAIConnector.close();
        gnomadConnector.close();

        sshTunnelService.close();
        instance = null;
    }

    public static Service getInstance() {
        return instance;
    }
}
