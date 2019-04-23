package org.forome.annotation.controller;

import com.google.common.base.Strings;
import htsjdk.variant.vcf.VCFFileReader;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        FormatAnfisaHttpClient formatAnfisaHttpClient;
        try {
            formatAnfisaHttpClient = new FormatAnfisaHttpClient();
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        }

        String sessionId = request.getParameter("session");
        if (sessionId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }
        Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
        if (userId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }

        TempVCFFile tempVCFFile = buildTempVCFFile(request);
        VCFFileReader vcfFileReader = tempVCFFile.vcfFileReader;

        AnnotatorResult annotatorResult = annotator.annotateJson(
                String.format("%s_wgs", "noname"),
                null,
                vcfFileReader, null,
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
                    tempVCFFile.close();
                }, () -> {
                    log.debug("FormatVcfController requestId: {}, 5", requestId);
                    JSONArray out = new JSONArray();
                    for (JSONObject jsonObject : ourResults) {
                        out.add(jsonObject);
                    }
                    completableFuture.complete(ResponseBuilder.build(out));
                    log.debug("FormatVcfController build response, time: {}", System.currentTimeMillis());
                    tempVCFFile.close();
                });
        return completableFuture;
    }

    private static class TempVCFFile implements AutoCloseable {

        public final VCFFileReader vcfFileReader;
        public final Path path;

        public TempVCFFile(VCFFileReader vcfFileReader, Path path) {
            this.vcfFileReader = vcfFileReader;
            this.path = path;
        }

        @Override
        public void close() {
            vcfFileReader.close();
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignore) {
            }
        }
    }

    private static TempVCFFile buildTempVCFFile(HttpServletRequest request) {
        Path vcfFile = null;
        try {
            vcfFile = Files.createTempFile("temp_", ".vcf");

            String pData = request.getParameter("data");
            if (!Strings.isNullOrEmpty(pData)) {
                Files.write(vcfFile, pData.getBytes(StandardCharsets.UTF_8));
            } else {
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

                multipartFile.transferTo(vcfFile.toFile());
            }

            return new TempVCFFile(
                    new VCFFileReader(vcfFile, false),
                    vcfFile
            );
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        } catch (Throwable e) {
            try {
                if (vcfFile != null) {
                    Files.deleteIfExists(vcfFile);
                }
            } catch (IOException ignore) {
            }
            throw e;
        }
    }
}
