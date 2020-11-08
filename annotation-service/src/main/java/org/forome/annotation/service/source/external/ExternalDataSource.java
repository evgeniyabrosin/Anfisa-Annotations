/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.service.source.external;

import org.forome.annotation.config.source.SourceExternalConfig;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.service.source.DataSource;
import org.forome.annotation.service.source.external.astorage.AStorageHttp;
import org.forome.annotation.service.source.external.httprequest.HttpRequest;
import org.forome.annotation.service.source.external.source.ExternalSource;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;

import java.io.IOException;
import java.net.URL;

public class ExternalDataSource implements DataSource {

	public final URL url;
	public final HttpRequest httpRequest;
	public final AStorageHttp aStorageHttp;


	public final LiftoverConnector liftoverConnector;

	public ExternalDataSource(SourceExternalConfig sourceHttpConfig) {
		this.url = buildUrl(sourceHttpConfig);
		this.httpRequest = new HttpRequest(url);
		try {
			this.aStorageHttp = new AStorageHttp(httpRequest);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			this.liftoverConnector = new LiftoverConnector();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Source getSource(Assembly assembly) {
		return new ExternalSource(this, assembly);
	}

	private final URL buildUrl(SourceExternalConfig sourceHttpConfig) {
		if (sourceHttpConfig.sshTunnelConfig == null) {
			return sourceHttpConfig.url;
		} else {
			try {
				SSHConnectService sshConnectService = new SSHConnectService();

				SshTunnelConfig sshTunnelConfig = sourceHttpConfig.sshTunnelConfig;
				SSHConnect sshTunnel = sshConnectService.getSSHConnect(
						sshTunnelConfig.host,
						sshTunnelConfig.port,
						sshTunnelConfig.user,
						sshTunnelConfig.key
				);
				int port = sshTunnel.getTunnel(sourceHttpConfig.url.getPort());

				return new URL(
						sourceHttpConfig.url.getProtocol(),
						sourceHttpConfig.url.getHost(),
						port,
						sourceHttpConfig.url.getFile()
				);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
