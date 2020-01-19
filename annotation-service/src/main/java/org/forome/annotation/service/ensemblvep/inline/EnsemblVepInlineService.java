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

package org.forome.annotation.service.ensemblvep.inline;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.ensemblvep.EnsemblVepConfig;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;
import org.forome.annotation.data.ref.RefConnector;
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

	@Override
	public CompletableFuture<JSONObject> getVepJson(Variant variant) {
		String reference = refConnector.getRef(variant);
		return getVepJson(variant.chromosome, variant.getStart(), variant.end, reference, variant.getStrAlt());
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(Chromosome chromosome, int start, int end, String alternative) {
		String reference = refConnector.getRef(chromosome, start, end);
		return getVepJson(chromosome, start, end, reference, alternative);
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(String id) {
		throw new RuntimeException();
	}

	private CompletableFuture<JSONObject> getVepJson(Chromosome chromosome, int start, int end, String reference, String alternative) {
		String request = buildRequest(chromosome, start, end, reference, alternative);
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

	@Override
	public void close() {
		isRun = false;
		ensemblVepRunner.close();
	}
}
