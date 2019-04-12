package org.forome.annotation.connector.anfisa;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.forome.annotation.Main;
import org.forome.annotation.exception.ExceptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class AnfisaHttpClient {

	private final static Logger log = LoggerFactory.getLogger(AnfisaHttpClient.class);

	private static String HOST="grch37.rest.ensembl.org";

	private final RequestConfig requestConfig;
	private final PoolingNHttpClientConnectionManager connectionManager;

	private final HttpHost httpHost;

	protected AnfisaHttpClient() throws IOException {
		requestConfig = RequestConfig.custom()
				.setConnectTimeout(2000)//Таймаут на подключение
				.setSocketTimeout(1 * 60 * 1000)//Таймаут между пакетами
				.setConnectionRequestTimeout(1 * 60 * 1000)//Таймаут на ответ
				.build();

		connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
		connectionManager.setMaxTotal(100);
		connectionManager.setDefaultMaxPerRoute(100);

		httpHost = new HttpHost(HOST);
	}

	protected CompletableFuture<JSONArray> request(String endpoint) {
		CompletableFuture<JSONArray> future = new CompletableFuture<>();

		try {

			HttpRequest httpRequest = new HttpGet(new URI("http://" + HOST + endpoint ));
			httpRequest.addHeader(new BasicHeader("Content-Type","application/json"));

			CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
					.setDefaultRequestConfig(requestConfig)
//					.setConnectionManager(connectionManager)
//					.setConnectionManagerShared(true)
					.build();
			httpclient.start();

			httpclient.execute(httpHost, httpRequest, new FutureCallback<HttpResponse>() {
				@Override
				public void completed(HttpResponse response) {
					try {
						HttpEntity entity = response.getEntity();
						String entityBody = EntityUtils.toString(entity);

						Object rawResponse;
						try {
							rawResponse = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(entityBody);
						} catch (Exception e) {
							throw ExceptionBuilder.buildExternalServiceException(e, "Error parse response, endpoint: " + endpoint + " response: '" + entityBody + "'");
						}

						if (rawResponse instanceof JSONArray) {
							future.complete((JSONArray) rawResponse);
						} else if (rawResponse instanceof JSONObject && ((JSONObject) rawResponse).containsKey("error")) {
							throw ExceptionBuilder.buildExternalServiceException(new RuntimeException(), "Error parse response, endpoint: " + endpoint + " response: '" + entityBody + "'");
						} else {
							Main.crash(new IOException("Unknown response, endpoint: " + endpoint + " response: '" + entityBody + "'"));
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
		} catch (Throwable ex) {
			log.error("Exception close connect", ex);
			future.completeExceptionally(ex);
		}

		return future;
	}
}
