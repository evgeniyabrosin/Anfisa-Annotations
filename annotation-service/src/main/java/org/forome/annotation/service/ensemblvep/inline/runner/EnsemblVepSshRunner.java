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
