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

    private static String getKeySSHConnect(String host, int port, String user) {
        return new StringBuilder(host).append(port).append(user).toString();
    }

    @Override
    public void close() {
        for (SSHConnect sshConnect : sshConnects.values()) {
            sshConnect.close();
        }
    }
}
