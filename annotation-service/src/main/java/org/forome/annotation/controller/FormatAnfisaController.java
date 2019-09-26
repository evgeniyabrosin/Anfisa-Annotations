package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.Service;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.format.FormatAnfisaHttpClient;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.network.authcontext.BuilderAuthContext;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.variant.vep.VariantVep;
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

        BuilderAuthContext builderAuthContext = new BuilderAuthContext(service);
        builderAuthContext.auth(request);

        String sRequestData = request.getParameter("data");
        if (Strings.isNullOrEmpty(sRequestData)) {
            throw ExceptionBuilder.buildInvalidValueException("data");
        }

        EnsemblVepService ensemblVepService = service.getEnsemblVepService();
        if (ensemblVepService == null) {
            throw ExceptionBuilder.buildInvalidOperation("inited");
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
                            ensemblVepService.getVepJson(requestItem.chromosome, requestItem.start, requestItem.end, requestItem.alternative)
                                    .thenApply(vepJson -> {
                                        VariantVep variantVep = new VariantVep(requestItem.chromosome, requestItem.start, requestItem.end);
                                        variantVep.setVepJson(vepJson);
                                        return anfisaConnector.build(new AnfisaInput.Builder().build(), variantVep);
                                    })
                                    .thenCompose(anfisaResult -> {
                                        JSONObject result = anfisaResult.toJSON();
                                        CompletableFuture<JSONArray> futureItem = formatAnfisaHttpClient.request(result.toJSONString())
                                                .exceptionally(throwable -> {
                                                    if (throwable instanceof AnnotatorException) {
                                                        throw (AnnotatorException) throwable;
                                                    } else {
                                                        Main.crash(throwable);
                                                        return null;
                                                    }
                                                });

                                        return futureItem
                                                .thenApply(v -> {
                                                    JSONObject out = new JSONObject();
                                                    out.put("input", new JSONArray() {{
                                                        add(requestItem.chromosome);
                                                        add(requestItem.start);
                                                        add(requestItem.end);
                                                        add(requestItem.alternative);
                                                    }});

                                                    JSONArray outs = new JSONArray();
                                                    JSONArray fresult = futureItem.join();
                                                    outs.add(fresult);
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
                                //TODO Ulitin V. Через пару дней необходимо удалить
                                log.error("TODO! Этого сообщения быть не должно", throwable);
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
