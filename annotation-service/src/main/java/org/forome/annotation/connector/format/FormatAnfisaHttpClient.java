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

package org.forome.annotation.connector.format;

import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.forome.annotation.exception.ExceptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FormatAnfisaHttpClient {

    private final static Logger log = LoggerFactory.getLogger(FormatAnfisaHttpClient.class);

    public static String HOST = "anfisa.forome.dev";

    private final RequestConfig requestConfig;
    private final PoolingNHttpClientConnectionManager connectionManager;

    private final HttpHost httpHost;
    private final CredentialsProvider credentialsProvider;

    public FormatAnfisaHttpClient() throws IOException {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)//Таймаут на подключение
                .setSocketTimeout(1 * 15 * 1000)//Таймаут между пакетами
                .setConnectionRequestTimeout(1 * 15 * 1000)//Таймаут на ответ
                .build();

        connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(100);

        httpHost = new HttpHost(HOST, 443, "https");

        //TODO Ulitin V. Необходимо решить проблему с авторизацией, ее поидее быть недолжно, необходимо общее размещение в защищенном контуре
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials("forome", "forome1!")
        );
    }

    public CompletableFuture<JSONArray> request(String anfisaResult) {
        CompletableFuture<JSONArray> future = new CompletableFuture<>();

        try {
            CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setDefaultCredentialsProvider(credentialsProvider)
//					.setConnectionManager(connectionManager)
//					.setConnectionManagerShared(true)
                    .build();
            httpclient.start();


            HttpPost httpPostRequest = new HttpPost(new URI("https://" + HOST + "/anfisa-anno/app/single_cnt"));
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("record", anfisaResult));
            httpPostRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));

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
                            future.complete((JSONArray)rawResponse);
                        } else {
                            throw ExceptionBuilder.buildExternalServiceException(
                                    new RuntimeException("Exception external service(Anfisa), response: " + entityBody),
                                    "Anfisa", "Response: " + entityBody
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
        } catch (Throwable ex) {
            log.error("Exception close connect", ex);
            future.completeExceptionally(ex);
        }

        return future;
    }
}
