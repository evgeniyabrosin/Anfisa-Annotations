package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.network.authcontext.BuilderAuthContext;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.forome.annotation.utils.ExecutorServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * http://localhost:8095/GetAnfisaJSON?session=...&data=[{"chromosome": "1", "start": 6484880, "end": 6484880, "alternative": "G"}]
 */
@Controller
@RequestMapping(value = {"/GetAnfisaData", "/annotationservice/GetAnfisaData", "/GetAnfisaJSON", "/annotationservice/GetAnfisaJSON"})
public class GetAnfisaJSONController {

    private final static Logger log = LoggerFactory.getLogger(GetAnfisaJSONController.class);

    public static class RequestItem {

        public final Chromosome chromosome;
        public final int start;
        public final int end;
        public final String alternative;

        public RequestItem(Chromosome chromosome, int start, int end, String alternative) {
            this.chromosome = chromosome;
            this.start = start;
            this.end = end;
            this.alternative = alternative;
        }
    }

    @RequestMapping(value = {"", "/"})
    public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
        Service service = Service.getInstance();

        BuilderAuthContext builderAuthContext = new BuilderAuthContext(service);
        builderAuthContext.auth(request);

        log.debug("GetAnfisaJSONController execute, time: {}", System.currentTimeMillis());

        String sRequestData = request.getParameter("data");
        if (Strings.isNullOrEmpty(sRequestData)) {
            throw ExceptionBuilder.buildInvalidValueException("data");
        }

        EnsemblVepService ensemblVepService = service.getEnsemblVepService();
        if (ensemblVepService == null) {
            throw ExceptionBuilder.buildInvalidOperation("inited");
        }

        CompletableFuture<JSONArray> future = new CompletableFuture<>();
        ExecutorServiceUtils.poolExecutor.execute(() -> {
            try {
                long t1 = System.currentTimeMillis();

                ArrayList<RequestItem> requestItems = parseRequestData(sRequestData);

                List<CompletableFuture<List<AnfisaResult>>> futureAnfisaResults = new ArrayList<>();
                AnfisaConnector anfisaConnector = service.getAnfisaConnector();
                for (RequestItem requestItem : requestItems) {
                    futureAnfisaResults.add(
                            ensemblVepService.getVepJson(requestItem.chromosome, requestItem.start, requestItem.end, requestItem.alternative)
                                    .thenApply(vepJson -> {
                                        VariantVep variantVep = new VariantCustom(requestItem.chromosome, requestItem.start, requestItem.end);
                                        variantVep.setVepJson(vepJson);
                                        return anfisaConnector.build(new AnfisaInput.Builder().build(), variantVep);
                                    })
                    );
                }

                CompletableFuture.allOf(futureAnfisaResults.toArray(new CompletableFuture[futureAnfisaResults.size()]))
                        .thenApply(v -> {
                            JSONArray results = new JSONArray();
                            for (int i = 0; i < requestItems.size(); i++) {
                                RequestItem requestItem = requestItems.get(i);

                                List<AnfisaResult> anfisaResults = futureAnfisaResults.get(i).join();
                                for (AnfisaResult anfisaResult: anfisaResults) {
                                    JSONObject result = new JSONObject();
                                    result.put("input", new JSONArray() {{
                                        add(requestItem.chromosome);
                                        add(requestItem.start);
                                        add(requestItem.end);
                                        add(requestItem.alternative);
                                    }});

                                    JSONArray outAnfisaResults = new JSONArray();
                                    outAnfisaResults.add(anfisaResult.toJSON());
                                    result.put("result", outAnfisaResults);

                                    results.add(result);
                                }
                            }

                            long t2 = System.currentTimeMillis();
                            log.debug("GetAnfisaJSONController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

                            future.complete(results);
                            return null;
                        })
                        .exceptionally(ex -> {
                            Throwable throwable = ex;
                            if (ex instanceof CompletionException) {
                                throwable = ex.getCause();
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
                    log.debug("GetAnfisaJSONController build response, time: {}", System.currentTimeMillis());
                    return responseEntity;

                })
                .exceptionally(throwable -> ResponseBuilder.build(throwable));
    }

    public static ArrayList<RequestItem> parseRequestData(String sRequestData) {
        ArrayList<RequestItem> requestItems = new ArrayList<>();
        JSONArray jRequestData;
        try {
            jRequestData = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(sRequestData);
        } catch (Throwable ex) {
            throw ExceptionBuilder.buildInvalidValueJsonException("data", ex);
        }
        for (Object item : jRequestData) {
            if (!(item instanceof JSONObject)) {
                throw ExceptionBuilder.buildInvalidValueException("data");
            }
            JSONObject oItem = (JSONObject) item;

            Chromosome chromosome = Chromosome.of(oItem.getAsString("chromosome"));

            int start = RequestParser.toInteger("start", oItem.getAsString("start"));

            int end = RequestParser.toInteger("end", oItem.getAsString("end"));

            String alternative = oItem.getAsString("alternative");
            if (Strings.isNullOrEmpty(alternative)) {
                throw ExceptionBuilder.buildInvalidValueException("alternative", alternative, "Description incomplete: please specify alteration (for example, AAC)");
            }
            if (alternative.contains(">")) {//TODO Добавить полноценную валидацию на входной параметр
                throw ExceptionBuilder.buildInvalidValueException("alternative", alternative, "Description incomplete: please specify alteration (for example, AAC)");
            }

            requestItems.add(new RequestItem(
                    chromosome,
                    start,
                    end,
                    alternative
            ));
        }

        return requestItems;
    }
}
