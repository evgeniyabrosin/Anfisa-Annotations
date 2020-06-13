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
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.mysql.ClinvarConnectorMysql;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gnomad.datasource.http.GnomadDataSourceHttp;
import org.forome.annotation.data.gtex.mysql.GTEXConnectorMysql;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.GTFConnectorImpl;
import org.forome.annotation.data.gtf.datasource.http.GTFDataSourceHttp;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.mysql.PharmGKBConnectorMysql;
import org.forome.annotation.data.ref.RefConnector;
import org.forome.annotation.data.sourceHttp38.SourceHttp38;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnectorImpl;
import org.forome.annotation.data.spliceai.datasource.http.SpliceAIDataSourceHttp;
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

	protected static GnomadConnectorImpl gnomadConnector;
	protected static SpliceAIConnector spliceAIConnector;
	protected static ConservationData conservationConnector;
	protected static HgmdConnector hgmdConnector;
	protected static ClinvarConnector clinvarConnector;
	protected static LiftoverConnector liftoverConnector;
	protected static GTFConnector gtfConnector;
	protected static RefConnector refConnector;
	protected static GTEXConnectorMysql gtexConnector;
	protected static PharmGKBConnector pharmGKBConnector;
	protected static SourceHttp38 sourceHttp38;
	protected static EnsemblVepService ensemblVepService;
	protected static AnfisaConnector anfisaConnector;

	@BeforeClass
	public static void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		sshTunnelService = new SSHConnectService();
		databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
//		gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});

		liftoverConnector = new LiftoverConnector();

		gnomadConnector = new GnomadConnectorImpl(
				new GnomadDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
				(t, e) -> {
					log.error("Fail", e);
					Assert.fail();
				}
		);
//		gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});

		spliceAIConnector = new SpliceAIConnectorImpl(
				new SpliceAIDataSourceHttp(liftoverConnector)
		);
//		spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

		conservationConnector = new ConservationData(databaseConnectService);

//		hgmdConnector = new HgmdConnectorHttp();
		hgmdConnector = new HgmdConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.hgmdConfigConnector);

//		clinvarConnector = new ClinvarConnectorHttp();
		clinvarConnector = new ClinvarConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.foromeConfigConnector);


		gtfConnector = new GTFConnectorImpl(
				new GTFDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
				(t, e) -> {
					log.error("Fail", e);
					Assert.fail();
				});
//		gtfConnector = new GTFConnectorImpl(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});
		refConnector = new RefConnector(databaseConnectService, serviceConfig.refConfigConnector);

		gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

//		pharmGKBConnector = new PharmGKBConnectorHttp();
		pharmGKBConnector = new PharmGKBConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

		sourceHttp38 = new SourceHttp38(
				databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector
		);

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
				pharmGKBConnector,
				sourceHttp38
		);
	}

	@AfterClass
	public static void destroy() {
		anfisaConnector.close();
		gtfConnector.close();
		liftoverConnector.close();
		clinvarConnector.close();
		hgmdConnector.close();
		spliceAIConnector.close();
		gnomadConnector.close();

		databaseConnectService.close();
		sshTunnelService.close();
	}
}
