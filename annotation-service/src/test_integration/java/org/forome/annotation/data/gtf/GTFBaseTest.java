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

package org.forome.annotation.data.gtf;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.gtf.datasource.http.GTFDataSourceHttp;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.source.SourceService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTFBaseTest {

	private final static Logger log = LoggerFactory.getLogger(GTFBaseTest.class);

	protected GTFConnector gtfConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
		LiftoverConnector liftoverConnector = new LiftoverConnector();
		SourceService sourceService = new SourceService(serviceConfig.sourceConfig);

		gtfConnector = new GTFConnectorImpl(
				new GTFDataSourceHttp(liftoverConnector, sourceService.dataSource),
				liftoverConnector,
				(t, e) -> {
					log.error("Fail", e);
					Assert.fail();
				});
//		gtfConnector = new GTFConnectorImpl(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> {
//			log.error("Fail", e);
//			Assert.fail();
//		});
	}
}
