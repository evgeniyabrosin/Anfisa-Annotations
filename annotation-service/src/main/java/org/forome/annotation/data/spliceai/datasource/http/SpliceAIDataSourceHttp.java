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

package org.forome.annotation.data.spliceai.datasource.http;

import net.minidev.json.JSONArray;
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
import org.forome.annotation.data.spliceai.datasource.SpliceAIDataSource;
import org.forome.annotation.data.spliceai.struct.Row;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.*;
import org.forome.annotation.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SpliceAIDataSourceHttp implements SpliceAIDataSource {

	private final static Logger log = LoggerFactory.getLogger(SpliceAIDataSourceHttp.class);

	private final LiftoverConnector liftoverConnector;

	private final DatabaseConnectService.AStorage aStorage;

	private final RequestConfig requestConfig;
	private final PoolingNHttpClientConnectionManager connectionManager;
	private final HttpHost httpHost;

	public SpliceAIDataSourceHttp(
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

	@Override
	public List<Row> getAll(Assembly assembly, String chromosome, int position, String ref, Allele altAllele) {
		Position pos38 = liftoverConnector.toHG38(assembly, new Position(Chromosome.of(chromosome), position));

		JSONObject response = request(
				String.format("http://%s:%s/get?array=hg38&loc=%s:%s", aStorage.host, aStorage.port, pos38.chromosome.getChromosome(), pos38.value)
		);
		JSONArray jRecords = (JSONArray) response.get("SpliceAI");
		if (jRecords == null) {
			return Collections.emptyList();
		}

		List<JSONObject> records = jRecords.stream()
				.map(o -> (JSONObject) o)
				.filter(item -> item.getAsString("REF").equals(ref) && item.getAsString("ALT").equals(altAllele.getBaseString()))
				.collect(Collectors.toList());

		return records.stream().map(jsonObject -> _build(pos38, jsonObject)).collect(Collectors.toList());
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return Collections.emptyList();
	}

	@Override
	public void close() {

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

	private static Row _build(Position pos38, JSONObject jsonObject) {
		String chrom = pos38.chromosome.getChar();
		int pos = pos38.value;
		String ref = jsonObject.getAsString("REF");
		String alt = jsonObject.getAsString("ALT");
		String symbol = jsonObject.getAsString("SYMBOL");
		String strand = "-";
		String type = "-";
		int dp_ag = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_AG"));
		int dp_al = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_AL"));
		int dp_dg = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_DG"));
		int dp_dl = MathUtils.toPrimitiveInteger(jsonObject.getAsNumber("DP_DL"));
		float ds_ag = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_AG"));
		float ds_al = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_AL"));
		float ds_dg = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_DG"));
		float ds_dl = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("DS_DL"));
		String id = jsonObject.getAsString("ID");
		float max_ds = MathUtils.toPrimitiveFloat(jsonObject.getAsNumber("MAX_DS"));

		return new Row(
				chrom, pos, ref, alt,
				symbol, strand, type,
				dp_ag, dp_al, dp_dg, dp_dl,
				ds_ag, ds_al, ds_dg, ds_dl,
				id, max_ds
		);
	}
}
