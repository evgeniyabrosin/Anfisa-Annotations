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

package org.forome.annotation.service.ensemblvep.inline.runner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;

public class EnsemblVepSshRunner extends EnsemblVepRunner {

	private final SSHConnectService sshTunnelService;
	private final SshTunnelConfig sshTunnelConfig;

	private Channel channel;

	public EnsemblVepSshRunner(SSHConnectService sshTunnelService, SshTunnelConfig sshTunnelConfig) throws Exception {
		super();
		this.sshTunnelService = sshTunnelService;
		this.sshTunnelConfig = sshTunnelConfig;
	}

	@Override
	protected synchronized void connect() throws Exception {
		SSHConnect sshConnect = sshTunnelService.getSSHConnect(
				sshTunnelConfig.host,
				sshTunnelConfig.port,
				sshTunnelConfig.user,
				sshTunnelConfig.key
		);

		channel = sshConnect.openChannel();
		((ChannelExec) channel).setCommand(cmd);

		stdin = channel.getOutputStream();
		stdout = channel.getInputStream();
		stderr = ((ChannelExec) channel).getErrStream();
		channel.connect();
	}

	@Override
	public void close() {
		channel.disconnect();
		super.close();
	}
}
