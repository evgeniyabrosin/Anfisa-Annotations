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

package org.forome.annotation.data.astorage;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.forome.annotation.config.connector.base.AStorageConfigConnector;
import org.forome.annotation.data.astorage.struct.AStorageSource;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.Statistics;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * curl -d '{"variants":[{"chrom":"chr3","pos":38603929}], "fasta":"hg38"}' -H "Content-Type: application/json" -X POST "localhost:8290/collect"
 * <p>
 * curl "localhost:8290/get?array=hg38&loc=3:38603929&alt=C"
 */
public class AStorageHttp {

	private final static Logger log = LoggerFactory.getLogger(AStorageHttp.class);

	private final LiftoverConnector liftoverConnector;

	private final DatabaseConnectService.AStoragePython aStorage;

	private final RequestConfig requestConfig;
	private final PoolingNHttpClientConnectionManager connectionManager;
	private final HttpHost httpHost;

	private final Statistics statistics;

	public AStorageHttp(
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

		this.statistics = new Statistics();
	}

	public AStorageSource get(Assembly assembly, Variant variant) {
		JSONObject params = new JSONObject();
		params.put("variants", new JSONArray() {{
			add(new JSONObject() {{
				put("chrom", variant.chromosome.getChromosome());
				put("pos", variant.getStart());
				put("last", (variant.getStart() < variant.end) ? variant.end : variant.getStart());
			}});
		}});
		if (assembly == Assembly.GRCh37) {
			params.put("fasta", "hg19");
		} else if (assembly == Assembly.GRCh38) {
			params.put("fasta", "hg38");
		} else {
			throw new RuntimeException("Unknown assembly: " + assembly);
		}
		params.put("arrays", new JSONArray() {{
			add("SpliceAI");
			add("dbNSFP");
			add("gnomAD");
			add("dbSNP");
			if (assembly == Assembly.GRCh37) {
				add("fasta/hg19");
			} else if (assembly == Assembly.GRCh38) {
				add("fasta/hg38");
			} else {
				throw new RuntimeException("Unknown assembly: " + assembly);
			}
		}});

		int attempts = 5;
		while (true) {
			JSONObject response = null;
			try {
				long t1 = System.currentTimeMillis();
				response = request(params);
				statistics.addTime(System.currentTimeMillis() - t1);
				return new AStorageSource(assembly, response);
			} catch (Throwable t) {
				if (attempts-- > 0) {
					log.error("Exception request, last attempts: {}", attempts, t);
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {
					}
					continue;
				} else {
					throw t;
				}
			} finally {
				if (assembly == Assembly.GRCh37) {
					int resultPos = response.getAsNumber("pos").intValue();
					if (variant.getStart() != resultPos) {
						throw new RuntimeException("request: " + params.toJSONString() + "response: " + response);
					}
				}
			}
		}
	}

	private JSONObject request(JSONObject params) {
		CompletableFuture<JSONObject> future = new CompletableFuture<>();
		try {
			CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
					.setDefaultRequestConfig(requestConfig)
					.build();
			httpclient.start();

			URI uri = new URI(String.format("http://%s:%s/collect", aStorage.host, aStorage.port));
			HttpPost httpPostRequest = new HttpPost(uri);
			httpPostRequest.setEntity(new StringEntity(params.toJSONString(), ContentType.APPLICATION_JSON));

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
						if (rawResponse instanceof JSONArray) {
							JSONObject jResponse = (JSONObject) ((JSONArray) rawResponse).get(0);
							future.complete(jResponse);
						} else {
							throw ExceptionBuilder.buildExternalServiceException(
									new RuntimeException("Exception external service(AStorage), request: " + uri
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

	public Statistics.Stat getStatistics() {
		return statistics.getStat();
	}
}
