/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation;

import com.infomaximum.querypool.*;
import org.forome.annotation.config.Config;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnector;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.database.DatabaseService;
import org.forome.annotation.database.entityobject.user.UserReadable;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.QueryPoolExceptionBuilder;
import org.forome.annotation.network.NetworkService;
import org.forome.annotation.network.component.UserEditableComponent;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
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
	private final QueryPool queryPool;

	private final Config config;
	private final ServiceConfig serviceConfig;
	private final SSHConnectService sshTunnelService;
	private final DatabaseConnectService databaseConnectService;
	private final DatabaseService databaseService;
	private final NetworkService networkService;

	private final GnomadConnector gnomadConnector;
	private final SpliceAIConnector spliceAIConnector;
	private final ConservationData conservationConnector;
	private final HgmdConnector hgmdConnector;
	private final ClinvarConnector clinvarConnector;
	private final LiftoverConnector liftoverConnector;
	private final GTFConnector gtfConnector;
	private final GTEXConnector gtexConnector;
	private final PharmGKBConnector pharmGKBConnector;
	private final EnsemblVepService ensemblVepService;
	private final AnfisaConnector anfisaConnector;

	private final NotificationService notificationService;

	public Service(ArgumentParser arguments, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) throws Exception {
		instance = this;

		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		this.queryPool = new QueryPool(new QueryPoolExceptionBuilder() , uncaughtExceptionHandler);

		this.config = new Config();
		this.serviceConfig = new ServiceConfig();
		this.sshTunnelService = new SSHConnectService();
		this.databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
		this.databaseService = new DatabaseService(this);
		this.networkService = new NetworkService(arguments.port, uncaughtExceptionHandler);

		if (serviceConfig.notificationSlackConfig != null) {
			this.notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
		} else {
			this.notificationService = null;
		}

//        this.gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, uncaughtExceptionHandler);
		this.gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, uncaughtExceptionHandler);
		this.spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
		this.conservationConnector = new ConservationData(databaseConnectService);
		this.hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
		this.clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
		this.liftoverConnector = new LiftoverConnector();
		this.gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, uncaughtExceptionHandler);
		this.gtexConnector = new GTEXConnector(databaseConnectService, serviceConfig.gtexConfigConnector);
		this.pharmGKBConnector = new PharmGKBConnector(databaseConnectService, serviceConfig.pharmGKBConfigConnector);
		this.ensemblVepService = new EnsemblVepExternalService(uncaughtExceptionHandler);
//        this.ensemblVepService = new EnsemblVepInlineService(
//                sshTunnelService,
//                serviceConfig.ensemblVepConfigConnector,
//                new RefConnector(databaseConnectService, serviceConfig.refConfigConnector)
//        );

		this.anfisaConnector = new AnfisaConnector(
				gnomadConnector,
				spliceAIConnector,
				conservationConnector,
				hgmdConnector,
				clinvarConnector,
				liftoverConnector,
				gtfConnector,
				gtexConnector,
				pharmGKBConnector
		);

		queryPool.execute(this.databaseService.getDomainObjectSource(), new Query<Void>() {

			private ReadableResource<UserReadable> userReadableResource;
			private UserEditableComponent userEditableComponent;

			@Override
			public void prepare(ResourceProvider resources) {
				userReadableResource = resources.getReadableResource(UserReadable.class);
				userEditableComponent = new UserEditableComponent(resources);
			}

			@Override
			public Void execute(QueryTransaction transaction) throws AnnotatorException {
				if (!userReadableResource.iterator(transaction).hasNext()) {
					userEditableComponent.create("admin", "b82nfGl5sdg", transaction);
				}
				return null;
			}
		}).get();
	}

	public QueryPool getQueryPool() {
		return queryPool;
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

	public EnsemblVepService getEnsemblVepService() {
		return ensemblVepService;
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
		spliceAIConnector.close();
		gnomadConnector.close();
		ensemblVepService.close();

		databaseConnectService.close();
		sshTunnelService.close();
		instance = null;
	}

	public static Service getInstance() {
		return instance;
	}
}
