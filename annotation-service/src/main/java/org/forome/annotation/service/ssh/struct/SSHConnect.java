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

package org.forome.annotation.service.ssh.struct;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SSHConnect implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(SSHConnect.class);

	private final String host;
	private final int port;
	private final String user;
	private final String key;
	private final Map<Integer, Integer> tunnels;

	private JSch jsch;
	private Session sshSession;

	public SSHConnect(String host, int port, String user, String key) throws JSchException {
		this.host = host;
		this.port = port;
		this.user = user;
		this.key = key;
		this.tunnels = new HashMap<>();

		connect();
	}

	private synchronized void connect() throws JSchException {
		jsch = new JSch();
		jsch.addIdentity(key);

		sshSession = jsch.getSession(user, host, port);

		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		config.put("Compression", "yes");
		config.put("ConnectionAttempts", "2");
		sshSession.setConfig(config);

		sshSession.connect();

		log.debug("Ssh connected by: {}", host);
	}

	/**
	 *
	 * @param remotePort
	 * @return localPort
	 * @throws JSchException
	 */
	public int getTunnel(int remotePort) throws JSchException {
		Integer localPort = tunnels.get(remotePort);
		if (localPort == null) {
			synchronized (this) {
				localPort = tunnels.get(remotePort);
				if (localPort == null) {
					localPort = findAvailablePort();
					localPort = sshSession.setPortForwardingL(localPort, "127.0.0.1", remotePort);
					tunnels.put(remotePort, localPort);
				}
			}
		}
		return localPort;
	}

	public Channel openChannel() throws JSchException {
		return sshSession.openChannel("exec");
	}

	public Channel openChannel(String type) throws JSchException {
		return sshSession.openChannel(type);
	}

	private synchronized void disconnect() {
		tunnels.clear();
		if (sshSession != null) {
			sshSession.disconnect();
			sshSession = null;
		}
		jsch = null;
	}

	@Override
	public void close() {
		disconnect();
	}

	private static int findAvailablePort() {
		for (int port = 3307; port < 4000; ++port) {
			if (isAvailablePort(port)) {
				return port;
			}
		}
		throw new RuntimeException("Not found available port");
	}

	private static boolean isAvailablePort(int port) {
		try (ServerSocket ss = new ServerSocket(port)) {
			ss.setReuseAddress(true);
		} catch (IOException ignored) {
			return false;
		}

		try (DatagramSocket ds = new DatagramSocket(port)) {
			ds.setReuseAddress(true);
		} catch (IOException ignored) {
			return false;
		}

		try (Socket s = new Socket("localhost", port)) {
			return false;
		} catch (IOException ignored) {
			return true;
		}
	}
}
