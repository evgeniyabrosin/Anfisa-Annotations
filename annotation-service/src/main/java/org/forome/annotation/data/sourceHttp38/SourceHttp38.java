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

package org.forome.annotation.data.sourceHttp38;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.forome.annotation.config.connector.base.AStorageConfigConnector;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SourceHttp38 {

	private final static Logger log = LoggerFactory.getLogger(SourceHttp38.class);

	private final LiftoverConnector liftoverConnector;

	private final DatabaseConnectService.AStorage aStorage;

	private final RequestConfig requestConfig;
	private final PoolingNHttpClientConnectionManager connectionManager;
	private final HttpHost httpHost;

	public SourceHttp38(
			DatabaseConnectService databaseConnectService,
			LiftoverConnector liftoverConnector,
			AStorageConfigConnector aStorageConfigConnector
	) throws IOReactorException {
		this.liftoverConnector = liftoverConnector;

		aStorage = databaseConnectService.getAStorage(aStorageConfigConnector);

		requestConfig = RequestConfig.custom()
				.setConnectTimeout(5000)//Таймаут на подключение
				.setSocketTimeout(10 * 60 * 1000)//Таймаут между пакетами
				.setConnectionRequestTimeout(10 * 60 * 1000)//Таймаут на ответ
				.build();

		connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
		connectionManager.setMaxTotal(100);
		connectionManager.setDefaultMaxPerRoute(100);

		httpHost = new HttpHost(aStorage.host, aStorage.port, "http");

	}

	public JSONObject get(Assembly assembly, Chromosome chromosome, int position) {
		Position pos38 = liftoverConnector.toHG38(assembly, new Position(chromosome, position));
		if (pos38 == null) {
			return new JSONObject();
		}

		int attempts = 5;
		while (true) {
			try {
				return request(
						String.format("http://%s:%s/get?array=hg38&loc=%s:%s", aStorage.host, aStorage.port, pos38.chromosome.getChromosome(), pos38.value)
				);
			} catch (Throwable t) {
				if (attempts-- > 0) {
					log.error("Exception request, last attempts: {}", attempts, t);
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {}
					continue;
				} else {
					throw t;
				}
			}
		}
	}

	private JSONObject request(String url) {
		CompletableFuture<JSONObject> future = new CompletableFuture<>();
		try {
			CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
					.setDefaultRequestConfig(requestConfig)
					.build();
			httpclient.start();

			HttpPost httpPostRequest = new HttpPost(new URI(url));

			httpclient.execute(httpHost, httpPostRequest, new FutureCallback<HttpResponse>() {
				@Override
				public void completed(HttpResponse response) {
					try {
						HttpEntity entity = response.getEntity();
						String entityBody = EntityUtils.toString(entity);

						Object rawResponse;
						try {
							rawResponse = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(entityBody);
						} catch (Exception e) {
							throw ExceptionBuilder.buildExternalServiceException(new RuntimeException("Exception parse response external service, response: " + entityBody));
						}
						if (rawResponse instanceof JSONObject) {
							future.complete((JSONObject) rawResponse);
						} else {
							throw ExceptionBuilder.buildExternalServiceException(
									new RuntimeException("Exception external service(AStorage), request: " + url
											+ ", response: " + entityBody),
									"AStorage", "Response: " + entityBody
							);
						}
					} catch (Throwable ex) {
						future.completeExceptionally(ex);
					}

					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}

				@Override
				public void failed(Exception ex) {
					future.completeExceptionally(ex);
					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}

				@Override
				public void cancelled() {
					future.cancel(true);
					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}
			});
		} catch (Throwable t) {
			log.error("Exception close connect", t);
			future.completeExceptionally(t);
		}

		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}
}
