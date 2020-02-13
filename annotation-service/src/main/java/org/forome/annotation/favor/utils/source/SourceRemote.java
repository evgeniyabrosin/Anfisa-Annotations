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

package org.forome.annotation.favor.utils.source;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.service.ssh.struct.SSHConnect;

import java.io.InputStream;

public class SourceRemote extends Source {

    private final SSHConnect sshTunnel;
    private final ChannelSftp sftp;

    private final String pathremoteFile;

    public SourceRemote(SSHConnectService sshConnectService, String pathremoteFile) {
        try{
            this.sshTunnel = sshConnectService.getSSHConnect(
                    "anfisa.forome.org",
                    22,
                    "vulitin",
                    "/home/kris/processtech/key/processtech_id_rsa"
            );

            this.sftp = (ChannelSftp) sshTunnel.openChannel("sftp");
            this.sftp.connect();

            this.pathremoteFile = pathremoteFile;

        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return sftp.get(pathremoteFile);
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        sftp.disconnect();
        sshTunnel.close();
    }
}
