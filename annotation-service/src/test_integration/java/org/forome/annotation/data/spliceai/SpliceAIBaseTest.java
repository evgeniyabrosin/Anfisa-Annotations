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

package org.forome.annotation.data.spliceai;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.spliceai.http.SpliceAIConnectorHttp;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpliceAIBaseTest {

	private final static Logger log = LoggerFactory.getLogger(SpliceAIBaseTest.class);

	protected SpliceAIConnector spliceAIConnector;

	@Before
	public void init() throws Throwable {
		ServiceConfig serviceConfig = new ServiceConfig();
		SSHConnectService sshTunnelService = new SSHConnectService();
		DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);

		spliceAIConnector = new SpliceAIConnectorHttp();
//		spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
	}
}
