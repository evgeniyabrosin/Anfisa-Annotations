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

package org.forome.annotation.service.ssh;

import com.jcraft.jsch.JSchException;
import org.forome.annotation.service.ssh.struct.SSHConnect;

import java.util.HashMap;
import java.util.Map;

public class SSHConnectService implements AutoCloseable {

	private Map<String, SSHConnect> sshConnects;

	public SSHConnectService() {
		this.sshConnects = new HashMap<String, SSHConnect>();
	}

	public SSHConnect getSSHConnect(String host, int port, String user, String key) throws JSchException {
		String keySSHConnect = getKeySSHConnect(host, port, user);
		SSHConnect sshConnect = sshConnects.get(keySSHConnect);
		if (sshConnect == null) {
			synchronized (sshConnects) {
				sshConnect = sshConnects.get(keySSHConnect);
				if (sshConnect == null) {
					sshConnect = new SSHConnect(host, port, user, key);
					sshConnects.put(keySSHConnect, sshConnect);
				}
			}
		}
		return sshConnect;
	}

	public static String getKeySSHConnect(String host, int port, String user) {
		return new StringBuilder(host).append(port).append(user).toString();
	}

	@Override
	public void close() {
		for (SSHConnect sshConnect : sshConnects.values()) {
			sshConnect.close();
		}
	}
}
