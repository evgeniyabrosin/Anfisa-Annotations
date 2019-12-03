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

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.GnomadConnectorImpl;
import org.forome.annotation.connector.gtex.GTEXConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.pharmgkb.PharmGKBConnector;
import org.forome.annotation.connector.ref.RefConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.inline.EnsemblVepInlineService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnfisaBaseTest {

	private final static Logger log = LoggerFactory.getLogger(AnfisaBaseTest.class);

	private static SSHConnectService sshTunnelService;
	private static DatabaseConnectService databaseConnectService;

	protected static GnomadConnector gnomadConnector;
	protected static SpliceAIConnector spliceAIConnector;
	protected static ConservationConnector conservationConnector;
	protected static HgmdConnector hgmdConnector;
	protected static ClinvarConnector clinvarConnector;
	protected static LiftoverConnector liftoverConnector;
	protected static GTFConnector gtfConnector;
	protected static RefConnector refConnector;
	protected static GTEXConnector gtexConnector;
	protected static PharmGKBConnector pharmGKBConnector;
	protected static EnsemblVepService ensemblVepService;
	protected static AnfisaConnector anfisaConnector;

	@BeforeClass
	public static void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		sshTunnelService = new SSHConnectService();
		databaseConnectService = new DatabaseConnectService(sshTunnelService);
//		gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});
		gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
		conservationConnector = new ConservationConnector(databaseConnectService, serviceConfig.conservationConfigConnector);
		hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
		clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
		liftoverConnector = new LiftoverConnector();
		gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> {
			log.error("Fail", e);
			Assert.fail();
		});
		refConnector = new RefConnector(databaseConnectService, serviceConfig.refConfigConnector);
		gtexConnector = new GTEXConnector(databaseConnectService, serviceConfig.gtexConfigConnector);
		pharmGKBConnector = new PharmGKBConnector(databaseConnectService, serviceConfig.pharmGKBConfigConnector);
		ensemblVepService = new EnsemblVepInlineService(sshTunnelService, serviceConfig.ensemblVepConfigConnector, refConnector);
		anfisaConnector = new AnfisaConnector(
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
	}

	@AfterClass
	public static void destroy() {
		anfisaConnector.close();
		gtfConnector.close();
		liftoverConnector.close();
		clinvarConnector.close();
		hgmdConnector.close();
		conservationConnector.close();
		spliceAIConnector.close();
		gnomadConnector.close();

		databaseConnectService.close();
		sshTunnelService.close();
	}
}
