package org.forome.annotation.controller;

import io.reactivex.Observable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.Service;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.format.FormatAnfisaHttpClient;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import pro.parseq.vcf.VcfExplorer;
import pro.parseq.vcf.exceptions.InvalidVcfFileException;
import pro.parseq.vcf.utils.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping(value = {"/FormatVcf", "/annotationservice/FormatVcf"})
public class FormatVcfController {

    private final static Logger log = LoggerFactory.getLogger(FormatVcfController.class);

    @RequestMapping(value = {"/get"})
    public CompletableFuture<ResponseEntity> get(HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString().toLowerCase();
        log.debug("FormatVcfController requestId: {}, time: {}", requestId, System.currentTimeMillis());

        Service service = Service.getInstance();
        AnfisaConnector anfisaConnector = service.getAnfisaConnector();
        if (anfisaConnector == null) {
            throw ExceptionBuilder.buildInvalidOperation("inited");
        }
        Annotator annotator = new Annotator(anfisaConnector);

        String sessionId = request.getParameter("session");
        if (sessionId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }
        Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
        if (userId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw ExceptionBuilder.buildNotMultipartRequestException();
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        Map.Entry<String, List<MultipartFile>> entry = multipartRequest.getMultiFileMap().entrySet().stream().findFirst().orElse(null);
        if (entry == null) {
            throw ExceptionBuilder.buildFileNotUploadException();
        }
        MultipartFile multipartFile = entry.getValue().stream().findFirst().orElse(null);
        if (multipartFile == null) {
            throw ExceptionBuilder.buildFileNotUploadException();
        }

        FormatAnfisaHttpClient formatAnfisaHttpClient;
        try {
            formatAnfisaHttpClient = new FormatAnfisaHttpClient();
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        }

        VcfExplorer vcfExplorer;
        try (InputStream is = multipartFile.getInputStream()) {
            VcfReader reader = new InputStreamVcfReader(is);
            VcfParser parser = new VcfParserImpl();
            vcfExplorer = new VcfExplorer(reader, parser);
            vcfExplorer.parse(FaultTolerance.FAIL_FAST);
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        } catch (InvalidVcfFileException e) {
            throw ExceptionBuilder.buildInvalidVcfFileException(e);
        }
        if (vcfExplorer.getVcfData().getDataLines().size()>100){
            throw ExceptionBuilder.buildLargeVcfFile(vcfExplorer.getVcfData().getDataLines().size(), 100);
        }

        AnnotatorResult annotatorResult = annotator.annotateJson(
                String.format("%s_wgs", entry.getKey()),
                null,
                vcfExplorer, null,
                0
        );

        CompletableFuture<ResponseEntity> completableFuture = new CompletableFuture<>();
        List<JSONObject> ourResults = Collections.synchronizedList(new ArrayList<JSONObject>());
        annotatorResult.observableAnfisaResult
                .map(anfisaResult -> {
                    log.debug("FormatVcfController requestId: {}, 1: {}", requestId, anfisaResult);
                    return new Object[]{anfisaResult, GetAnfisaJSONController.build(anfisaResult)};
                })
                .flatMap(result ->
                        Observable.fromFuture(formatAnfisaHttpClient.request(((JSONObject) result[1]).toJSONString())
                                .thenApply(jsonArray -> {
                                    log.debug("FormatVcfController requestId: {}, 2: {}", requestId, jsonArray);
                                    return new Object[]{result[0], jsonArray};
                                }).exceptionally(throwable -> {
                                        Main.crash(throwable);
                                        return null;
                                })
                        )
                )
                .map(objects -> {
                    AnfisaResult anfisaResult = (AnfisaResult) objects[0];
                    JSONArray results = (JSONArray) objects[1];
                    log.debug("FormatVcfController requestId: {}, 3: {}", requestId, results.toJSONString().length());

                    JSONObject out = new JSONObject();
                    out.put("input", new JSONArray() {{
                        add(anfisaResult.filters.chromosome);
                        add(anfisaResult.data.start);
                        add(anfisaResult.data.end);
                        add(anfisaResult.data.alt);
                    }});
                    out.put("result", new JSONArray() {{
                        add(results);
                    }});
                    return out;
                })
                .subscribe(jsonArray -> {
                    log.debug("FormatVcfController requestId: {}, 4: {}", requestId, jsonArray.toJSONString().length());
                    ourResults.add(jsonArray);
                }, throwable -> {
                    log.error("Exception execute request", throwable);
                    completableFuture.completeExceptionally(throwable);
                }, () -> {
                    log.debug("FormatVcfController requestId: {}, 5", requestId);
                    JSONArray out = new JSONArray();
                    for (JSONObject jsonObject : ourResults) {
                        out.add(jsonObject);
                    }
                    completableFuture.complete(ResponseBuilder.build(out));
                    log.debug("FormatVcfController build response, time: {}", System.currentTimeMillis());
                });
        return completableFuture;
    }
}
