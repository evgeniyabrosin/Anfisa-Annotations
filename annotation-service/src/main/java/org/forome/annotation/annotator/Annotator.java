package org.forome.annotation.annotator;

import io.reactivex.Observable;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.annotator.utils.CaseUtils;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.parseq.vcf.VcfExplorer;
import pro.parseq.vcf.exceptions.InvalidVcfFileException;
import pro.parseq.vcf.types.DataLine;
import pro.parseq.vcf.types.Variant;
import pro.parseq.vcf.types.VcfLine;
import pro.parseq.vcf.utils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Annotator {

    private final static Logger log = LoggerFactory.getLogger(Annotator.class);

    private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private final AnfisaConnector anfisaConnector;

    public Annotator(AnfisaConnector anfisaConnector) {
        this.anfisaConnector = anfisaConnector;
    }

    public AnnotatorResult exec(
            String caseName,
            Path pathFam,
            Path pathVepFilteredVcf,
            Path pathVepFilteredVepJson,
            int startPosition
    ) throws IOException, ParseException, InvalidVcfFileException {
        if (!Files.exists(pathFam)) {
            throw new RuntimeException("Fam file is not exists: " + pathFam.toAbsolutePath());
        }
        if (!pathFam.getFileName().toString().endsWith(".fam")) {
            throw new IllegalArgumentException("Bad name fam file: " + pathFam.toAbsolutePath());
        }

        if (!Files.exists(pathVepFilteredVcf)) {
            throw new RuntimeException("Vcf file is not exists: " + pathVepFilteredVcf.toAbsolutePath());
        }
        if (!pathVepFilteredVcf.getFileName().toString().endsWith(".vcf")) {
            throw new IllegalArgumentException("Bad name vcf file (Need *.vcf): " + pathVepFilteredVcf.toAbsolutePath());
        }

        if (pathVepFilteredVepJson != null) {
            if (!Files.exists(pathVepFilteredVepJson)) {
                throw new RuntimeException("VepJson file is not exists: " + pathVepFilteredVepJson.toAbsolutePath());
            }
            if (!pathVepFilteredVepJson.getFileName().toString().endsWith(".vep.json")) {
                throw new IllegalArgumentException("Bad name pathVepFilteredVepJson file (Need *.vep.json): " + pathVepFilteredVepJson.toAbsolutePath());
            }
        }

        try (
                InputStream isFam = Files.newInputStream(pathFam);
                InputStream isVepFilteredVcf = Files.newInputStream(pathVepFilteredVcf);
                InputStream isVepFilteredVepJson = (pathVepFilteredVepJson != null) ? Files.newInputStream(pathVepFilteredVepJson): null
        ) {
            return exec(
                    caseName,
                    isFam,
                    isVepFilteredVcf,
                    isVepFilteredVepJson,
                    startPosition
            );
        }
    }

    public AnnotatorResult exec(
            String caseName,
            InputStream isFam,
            InputStream isVepFilteredVcf,
            InputStream isVepFilteredVepJson,
            int startPosition
    ) throws IOException, ParseException, InvalidVcfFileException {

        List<JSONObject> vepFilteredVepJsons = null;
        if (isVepFilteredVepJson != null) {
            vepFilteredVepJsons = new ArrayList<>();
            try (BufferedReader isBVepJson = new BufferedReader(new InputStreamReader(isVepFilteredVepJson))) {
                String line;
                while ((line = isBVepJson.readLine()) != null) {
                    JSONObject json = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(line);
                    vepFilteredVepJsons.add(json);
                }
            }
        }

        VcfReader reader = new InputStreamVcfReader(isVepFilteredVcf);
        VcfParser parser = new VcfParserImpl();
        VcfExplorer vcfExplorer = new VcfExplorer(reader, parser);
        vcfExplorer.parse(FaultTolerance.FAIL_SAFE);

        if (vepFilteredVepJsons != null && vepFilteredVepJsons.size() != vcfExplorer.getVcfData().getDataLines().size()) {
            throw new RuntimeException(
                    String.format("Not equal record size VepJsons(%s) and Vcf file(%s)",
                            vepFilteredVepJsons.size(), vcfExplorer.getVcfData().getDataLines().size()
                    )
            );
        }

        Map<String, Sample> samples = CaseUtils.parseFamFile(isFam);

        return annotateJson(
                String.format("%s_wgs", caseName),
                vepFilteredVepJsons,
                vcfExplorer, samples,
                startPosition
        );
    }

    public AnnotatorResult exec(
            String caseName,
            InputStream isVcf,
            int startPosition
    ) throws InvalidVcfFileException {

        VcfReader reader = new InputStreamVcfReader(isVcf);
        VcfParser parser = new VcfParserImpl();
        VcfExplorer vcfExplorer = new VcfExplorer(reader, parser);
        vcfExplorer.parse(FaultTolerance.FAIL_FAST);

        Map<String, Sample> samples = null; //CaseUtils.parseFamFile(isFam);

        return annotateJson(
                String.format("%s_wgs", caseName),
                null,
                vcfExplorer, samples,
                startPosition
        );
    }

    private AnnotatorResult annotateJson(
            String caseSequence,
            List<JSONObject> vepFilteredVepJsons,
            VcfExplorer vcfExplorer, Map<String, Sample> samples,
            int startPosition
    ) {
        return new AnnotatorResult(
                AnnotatorResult.Metadata.build(caseSequence, vcfExplorer, samples),
                Observable.create(o -> {
                    try {
                        ExecutorService threadPool = new DefaultThreadPoolExecutor(
                                MAX_THREAD_COUNT,
                                MAX_THREAD_COUNT,
                                0L,
                                TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<>(),
                                "AnnotatorExecutorQueue",
                                (t, e) -> {
                                    o.tryOnError(e);
                                }
                        );

                        List<CompletableFuture<AnfisaResult>> futures = new ArrayList<>();
                        List<VcfLine> vcfLines = vcfExplorer.getVcfData().getDataLines();
                        for (int i = startPosition; i < vcfLines.size(); i++) {
                            CompletableFuture<AnfisaResult> future = new CompletableFuture();
                            int finalI = i;
                            threadPool.submit(() -> {
                                try {
                                    DataLine dataLine = (DataLine) vcfLines.get(finalI);

                                    if (vepFilteredVepJsons != null) {
                                        JSONObject json = vepFilteredVepJsons.get(finalI);
                                        String chromosome = RequestParser.toChromosome(json.getAsString("seq_region_name"));
                                        long start = json.getAsNumber("start").longValue();
                                        long end = json.getAsNumber("end").longValue();

                                        AnfisaResult anfisaResult = anfisaConnector.build(caseSequence, chromosome, start, end, json, dataLine, samples);
                                        future.complete(anfisaResult);
                                    } else {
                                        Variant variant = dataLine.getVariants().stream().findFirst().orElse(null);
                                        if (variant == null) {
                                            future.completeExceptionally(new RuntimeException("Variant is empty"));
                                        }
                                        String chromosome = RequestParser.toChromosome(variant.getChrom());
                                        long start = variant.getPos();
                                        long end = variant.getPos();
                                        String alternative = variant.getAlt();

                                        anfisaConnector.request(chromosome, start, end, alternative)
                                                .thenApply(anfisaResults -> {
                                                    future.complete(anfisaResults.get(0));
                                                    return null;
                                                });
                                    }
                                } catch (Throwable e) {
                                    future.completeExceptionally(e);
                                }
                            });
                            futures.add(future);
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    for (CompletableFuture<AnfisaResult> future : futures) {
                                        o.onNext(future.get());
                                    }
                                    o.onComplete();
                                } catch (Throwable t) {
                                    o.tryOnError(t);
                                }
                            }
                        }).start();

                    } catch (Throwable t) {
                        o.tryOnError(t);
                    }
                })
        );
    }

}
