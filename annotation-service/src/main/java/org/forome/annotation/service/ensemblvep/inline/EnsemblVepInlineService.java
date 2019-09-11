package org.forome.annotation.service.ensemblvep.inline;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.ensemblvep.EnsemblVepConfig;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.connector.ref.RefConnector;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.inline.runner.EnsemblVepRunner;
import org.forome.annotation.service.ensemblvep.inline.runner.EnsemblVepSshRunner;
import org.forome.annotation.service.ensemblvep.inline.struct.EnsembleVepRequest;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class EnsemblVepInlineService implements EnsemblVepService {

    private final static Logger log = LoggerFactory.getLogger(EnsemblVepInlineService.class);

    private static Duration TIMEOUT = Duration.ofSeconds(5);

    private final EnsemblVepRunner ensemblVepRunner;
    private final RefConnector refConnector;

    private final ConcurrentMap<String, EnsembleVepRequest> requests;

    private boolean isRun = true;

    public EnsemblVepInlineService(
            SSHConnectService sshTunnelService,
            EnsemblVepConfig ensemblVepConfigConnector,
            RefConnector refConnector
    ) throws Exception {
        SshTunnelConfig sshTunnelConfig = ensemblVepConfigConnector.sshTunnelConfig;
        if (sshTunnelConfig != null) {
            ensemblVepRunner = new EnsemblVepSshRunner(sshTunnelService, sshTunnelConfig);
        } else {
            ensemblVepRunner = new EnsemblVepRunner();
        }
        ensemblVepRunner.start((request, response) -> eventNewResponse(request, response));
        this.requests = new ConcurrentHashMap<>();

        this.refConnector = refConnector;

        Thread thread = new Thread(() -> {
            while (isRun) {
                Instant now = Instant.now();
                for (EnsembleVepRequest ensembleVepRequest : requests.values()) {
                    if (Duration.between(now, ensembleVepRequest.getTime()).isNegative()) {
                        try {
                            log.debug("repeat request: {}", ensembleVepRequest.request);
                            ensemblVepRunner.send(ensembleVepRequest);
                        } catch (IOException e) {
                            log.debug("Exception", e);
                        }
                    }
                }
                try {
                    Thread.sleep(TIMEOUT.toMillis());
                } catch (InterruptedException ignore) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public CompletableFuture<JSONObject> getVepJson(Variant variant, String alternative) {
        String reference = refConnector.getRef(variant);
        return getVepJson(variant, reference, alternative);
    }

    @Override
    public CompletableFuture<JSONObject> getVepJson(Variant variant, String reference, String alternative) {
        String request = buildRequest(variant.chromosome, variant.start, variant.end, reference, alternative);
        synchronized (this) {
            EnsembleVepRequest ensembleVepRequest = requests.get(request);
            if (ensembleVepRequest == null) {
                ensembleVepRequest = new EnsembleVepRequest(
                        request, new CompletableFuture<JSONObject>()
                );
                requests.put(request, ensembleVepRequest);
                try {
                    ensemblVepRunner.send(ensembleVepRequest);
                } catch (IOException e) {
                    requests.remove(ensembleVepRequest);
                    ensembleVepRequest.future.completeExceptionally(e);
                }
            }
            return ensembleVepRequest.future;
        }
    }

    private static String buildRequest(Chromosome chromosome, int start, int end, String reference, String alternative) {
        return new StringBuilder()
                .append(chromosome.getChar()).append('\t')
                .append(start).append('\t').append(end).append('\t')
                .append(reference).append('/').append(alternative).toString();
    }

    private void eventNewResponse(String request, JSONObject response) {
        synchronized (this) {
            EnsembleVepRequest ensembleVepRequest = requests.remove(request);
            if (ensembleVepRequest != null) {
                ensembleVepRequest.future.complete(response);
            }
        }
    }

    /**
     * public synchronized void connect() throws JSchException, IOException {
     * SshTunnelConfig sshTunnelConfig = ensemblVepConfigConnector.sshTunnelConfig;
     * if (sshTunnelConfig != null) {
     * SSHConnect sshConnect = sshTunnelService.getSSHConnect(
     * sshTunnelConfig.host,
     * sshTunnelConfig.port,
     * sshTunnelConfig.user,
     * sshTunnelConfig.key
     * );
     * <p>
     * Channel channel = sshConnect.openChannel();
     * ((ChannelExec) channel).setCommand(cmd);
     * <p>
     * OutputStream stdin = channel.getOutputStream();
     * InputStream stdout = channel.getInputStream();
     * InputStream stderr = ((ChannelExec) channel).getErrStream();
     * ensemblVepRunner = new EnsemblVepRunner(stdin, stdout, stderr);
     * <p>
     * channel.connect();
     * <p>
     * <p>
     * <p>
     * for (int i=0; i< 10; i++) {
     * //            try {
     * //                Thread.sleep(100L);
     * //            } catch (InterruptedException e) {}
     * System.out.println("send, conn: " + channel.isConnected());
     * stdin.write("1\t881907\t881906\t-/C\t+\n".getBytes(StandardCharsets.UTF_8));
     * stdin.flush();
     * }
     * <p>
     * for (int i=0; i< 20; i++) {
     * try {
     * Thread.sleep(500L);
     * } catch (InterruptedException e) {}
     * System.out.println("send \\n, conn: " + channel.isConnected());
     * stdin.write("\n".getBytes(StandardCharsets.UTF_8));
     * //                stdin.write("M\t881907\t881906\t-/C\t+\r\n".getBytes(StandardCharsets.UTF_8));
     * stdin.flush();
     * }
     * <p>
     * System.out.println("Closing, conn: " + channel.isConnected());
     * ((ChannelExec) channel).disconnect();
     * System.out.println("Closed, conn: " + channel.isConnected());
     * }
     * <p>
     * <p>
     * new Thread(() -> {
     * try {
     * InputStreamReader inputReader = new InputStreamReader(stdout);
     * BufferedReader bufferedReader = new BufferedReader(inputReader);
     * String line = null;
     * <p>
     * while ((line = bufferedReader.readLine()) != null) {
     * System.out.println(System.currentTimeMillis() + " stdout: " + line);
     * }
     * bufferedReader.close();
     * inputReader.close();
     * } catch (IOException ex) {
     * ex.printStackTrace();
     * }
     * }).start();
     * <p>
     * new Thread(() -> {
     * try {
     * InputStreamReader inputReader = new InputStreamReader(stderr);
     * BufferedReader bufferedReader = new BufferedReader(inputReader);
     * String line = null;
     * <p>
     * while ((line = bufferedReader.readLine()) != null) {
     * System.out.println(System.currentTimeMillis() + " stderr: " + line);
     * }
     * bufferedReader.close();
     * inputReader.close();
     * } catch (IOException ex) {
     * ex.printStackTrace();
     * }
     * }).start();
     * }
     */

    @Override
    public void close() {
        isRun = false;
        ensemblVepRunner.close();
    }
}
