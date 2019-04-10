package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.format.FormatAnfisaHttpClient;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.utils.ExecutorServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * http://localhost:8095/FormatAnfisa?session=...&data=[{"chromosome": "1", "start": 6484880, "end": 6484880, "alternative": "G"}]
 */
@Controller
@RequestMapping(value = {"/FormatAnfisa", "/annotationservice/FormatAnfisa"})
public class FormatAnfisaController {

    private final static Logger log = LoggerFactory.getLogger(FormatAnfisaController.class);

    @RequestMapping(value = {"/get"})
    public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
        log.debug("FormatAnfisaController execute, time: {}", System.currentTimeMillis());

        Service service = Service.getInstance();

        String sessionId = request.getParameter("session");
        if (sessionId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }
        Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
        if (userId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }

        String sRequestData = request.getParameter("data");
        if (Strings.isNullOrEmpty(sRequestData)) {
            throw ExceptionBuilder.buildInvalidValueException("data");
        }

        AnfisaConnector anfisaConnector = service.getAnfisaConnector();
        if (anfisaConnector == null) {
            throw ExceptionBuilder.buildInvalidOperation("inited");
        }

        CompletableFuture<JSONArray> future = new CompletableFuture<>();
        ExecutorServiceUtils.poolExecutor.execute(() -> {
            try {
                long t1 = System.currentTimeMillis();

                FormatAnfisaHttpClient formatAnfisaHttpClient = new FormatAnfisaHttpClient();

                ArrayList<GetAnfisaJSONController.RequestItem> requestItems = GetAnfisaJSONController.parseRequestData(sRequestData);

                List<CompletableFuture<JSONObject>> futureAnfisaResults = new ArrayList<>();
                for (GetAnfisaJSONController.RequestItem requestItem : requestItems) {
                    futureAnfisaResults.add(
                            anfisaConnector.request(
                                    requestItem.chromosome,
                                    requestItem.start,
                                    requestItem.end,
                                    requestItem.alternative
                            )
                                    .thenCompose(anfisaResults -> {
                                        List<CompletableFuture<JSONArray>> futureItems = new ArrayList<>();
                                        for (AnfisaResult anfisaResult : anfisaResults) {
                                            JSONObject result = GetAnfisaJSONController.build(anfisaResult);
                                            CompletableFuture<JSONArray> futureItem = formatAnfisaHttpClient.request(result.toJSONString())
                                                    .thenApply(body -> {
                                                        Object rawResponse;
                                                        try {
                                                            rawResponse = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(body);
                                                        } catch (Exception e) {
                                                            throw new RuntimeException("Error parse response, body='" + body + "'", e);
                                                        }
                                                        if (rawResponse instanceof JSONArray) {
                                                            return (JSONArray) rawResponse;
                                                        } else {
                                                            throw ExceptionBuilder.buildExternalServiceException(new RuntimeException("Exception external service, body: " + body));
                                                        }
                                                    });
                                            futureItems.add(futureItem);
                                        }

                                        return CompletableFuture.allOf(futureItems.toArray(new CompletableFuture[futureItems.size()]))
                                                .thenApply(v -> {
                                                    JSONObject out = new JSONObject();
                                                    out.put("input", new JSONArray() {{
                                                        add(requestItem.chromosome);
                                                        add(requestItem.start);
                                                        add(requestItem.end);
                                                        add(requestItem.alternative);
                                                    }});

                                                    JSONArray outs = new JSONArray();
                                                    for (CompletableFuture<JSONArray> futureItem : futureItems) {
                                                        JSONArray result = futureItem.join();
                                                        outs.add(result);
                                                    }
                                                    out.put("result", outs);
                                                    return out;
                                                });
                                    })
                    );
                }

                CompletableFuture.allOf(futureAnfisaResults.toArray(new CompletableFuture[futureAnfisaResults.size()]))
                        .thenApply(v -> {
                            JSONArray results = new JSONArray();
                            for (int i = 0; i < requestItems.size(); i++) {
                                JSONObject result = futureAnfisaResults.get(i).join();
                                results.add(result);
                            }

                            long t2 = System.currentTimeMillis();
                            log.debug("FormatAnfisaController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

                            future.complete(results);
                            return null;
                        })
                        .exceptionally(ex -> {
                            Throwable throwable = ex;
                            if (ex instanceof CompletionException) {
                                throwable = ex.getCause();
                            }
                            if (throwable instanceof IOException) {
                                throwable = ExceptionBuilder.buildExternalServiceException(throwable, throwable.getMessage());
                            }
                            log.error("Exception execute request", throwable);
                            future.completeExceptionally(throwable);
                            return null;
                        });
            } catch (Throwable ex) {
                log.error("Exception execute request", ex);
                future.completeExceptionally(ex);
            }
        });

        return future
                .thenApply(out -> {
                    ResponseEntity responseEntity = ResponseBuilder.build(out);
                    log.debug("FormatAnfisaController build response, time: {}", System.currentTimeMillis());
                    return responseEntity;

                })
                .exceptionally(throwable -> ResponseBuilder.build(throwable));
    }
}
