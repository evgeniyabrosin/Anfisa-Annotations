package org.forome.annotation.annotator;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import io.reactivex.Observable;
import net.minidev.json.JSONObject;
import org.forome.annotation.annotator.input.VepJsonIterator;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.annotator.utils.CaseUtils;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    ) throws IOException {
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

        VepJsonIterator vepJsonIterator = null;
        if (pathVepFilteredVepJson != null) {
            if (!Files.exists(pathVepFilteredVepJson)) {
                throw new RuntimeException("VepJson file is not exists: " + pathVepFilteredVepJson.toAbsolutePath());
            }
            String vepJsonFIleName = pathVepFilteredVepJson.getFileName().toString();
            if (!(vepJsonFIleName.endsWith(".json") || vepJsonFIleName.endsWith(".json.gz"))) {
                throw new IllegalArgumentException("Bad name pathVepFilteredVepJson file (Need *.json vs *.json.gz): " + pathVepFilteredVepJson.toAbsolutePath());
            }
            vepJsonIterator = new VepJsonIterator(pathVepFilteredVepJson);
        }

        try (InputStream isFam = Files.newInputStream(pathFam)) {
            return exec(
                    caseName,
                    isFam,
                    pathVepFilteredVcf,
                    vepJsonIterator,
                    startPosition
            );
        }
    }

    public AnnotatorResult exec(
            String caseName,
            InputStream isFam,
            Path pathVepFilteredVcf,
            VepJsonIterator vepJsonIterator,
            int startPosition
    ) throws IOException {

        VCFFileReader vcfFileReader = new VCFFileReader(pathVepFilteredVcf, false);

        Map<String, Sample> samples = CaseUtils.parseFamFile(isFam);

        String platform;
        Set<String> x = Arrays.stream(pathVepFilteredVcf.getFileName().toString().toLowerCase().split("_"))
                .collect(Collectors.toSet());
        if (x.contains("wgs")) {
            platform = "wgs";
        } else if (x.contains("wes")) {
            platform = "wes";
        } else {
            platform = "wgs";
            log.warn("Could not determine platform (WES or WGS), assuming: " + platform);
        }
        String caseId = String.format("%s_%s", caseName, platform);

        return annotateJson(
                caseId,
                vepJsonIterator,
                vcfFileReader, samples,
                startPosition
        );
    }

    public AnnotatorResult annotateJson(
            String caseSequence,
            VepJsonIterator vepJsonIterator,
            VCFFileReader vcfFileReader, Map<String, Sample> samples,
            int startPosition
    ) {
        return new AnnotatorResult(
                AnnotatorResult.Metadata.build(caseSequence, vcfFileReader, samples),
                Observable.create(o -> {
                    try {
                        ExecutorService threadPool = new DefaultThreadPoolExecutor(
                                1,//MAX_THREAD_COUNT, TODO - нет поддержки распаралеливания!
                                1,//MAX_THREAD_COUNT,
                                0L,
                                TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<>(),
                                "AnnotatorExecutorQueue",
                                (t, e) -> {
                                    o.tryOnError(e);
                                }
                        );

                        List<CompletableFuture<AnfisaResult>> futures = new ArrayList<>();
                        try (CloseableIterator<VariantContext> iterator = vcfFileReader.iterator()) {
                            int i = 0;
                            while (i + 1 < startPosition) {
                                i++;
                                iterator.next();
                                vepJsonIterator.next();
                            }

                            while (iterator.hasNext()) {
                                VariantContext variantContext = iterator.next();

                                JSONObject vepJson = null;
                                if (vepJsonIterator == null) {
                                    //TODO https://rm.processtech.ru/issues/1046
                                    Thread.sleep(110L);
                                } else {
                                    vepJson = vepJsonIterator.next();
                                }

                                CompletableFuture<AnfisaResult> future = new CompletableFuture();
                                int finalI = i;
                                JSONObject finalVepJson = vepJson;
                                threadPool.submit(() -> {
                                    try {
                                        if (vepJsonIterator != null) {
                                            String chromosome = RequestParser.toChromosome(finalVepJson.getAsString("seq_region_name"));
                                            long start = finalVepJson.getAsNumber("start").longValue();
                                            long end = finalVepJson.getAsNumber("end").longValue();

                                            AnfisaResult anfisaResult = anfisaConnector.build(caseSequence, chromosome, start, end, finalVepJson, variantContext, samples);
                                            future.complete(anfisaResult);
                                        } else {
                                            String chromosome = RequestParser.toChromosome(variantContext.getContig());
                                            long start = variantContext.getStart();
                                            long end = variantContext.getEnd();

                                            //variantContext.getAltAlleleWithHighestAlleleCount();
                                            Allele allele = variantContext.getAlternateAlleles().stream()
                                                    .filter(iAllele -> !iAllele.getDisplayString().equals("*"))
                                                    .max(Comparator.comparing(variantContext::getCalledChrCount))
                                                    .orElse(null);
                                            if (allele == null) {
                                                future.completeExceptionally(new RuntimeException());
                                            }
                                            String alternative = allele.getDisplayString();

                                            log.debug("annotate ({}) 1: {}-{}", finalI, chromosome, start);

                                            anfisaConnector.request(chromosome, start, end, alternative)
                                                    .thenApply(anfisaResults -> {
                                                        log.debug("annotate ({}) 2: {}-{}", finalI, chromosome, start);
                                                        future.complete(anfisaResults.get(0));
                                                        return null;
                                                    })
                                                    .exceptionally(throwable -> {
                                                        future.completeExceptionally(throwable);
                                                        return null;
                                                    });
                                        }
                                    } catch (Throwable e) {
                                        log.error("annotate exception", e);
                                        future.completeExceptionally(e);
                                    }
                                });
                                futures.add(future);
                                i++;
                            }

                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    for (int i = 0; i < futures.size(); i++) {
                                        CompletableFuture<AnfisaResult> future = futures.get(i);
                                        try {
                                            AnfisaResult anfisaResult = future.join();
                                            o.onNext(anfisaResult);
                                            log.debug("annotate onNext() {}", i);
                                        } catch (CompletionException e) {
                                            //TODO https://rm.processtech.ru/issues/1048
                                            log.error("Exception processing", e);
                                        }
                                    }
                                    log.debug("annotate Complete");
                                    o.onComplete();
                                } catch (Throwable t) {
                                    log.debug("annotate Throwable", t);
                                    o.tryOnError(t);
                                }
                                if (vcfFileReader != null) {
                                    try {
                                        vcfFileReader.close();
                                    } catch (Throwable t) {
                                        log.debug("Exception close vcfFileReader", t);
                                    }
                                }
                                if (vepJsonIterator != null) {
                                    try {
                                        vepJsonIterator.close();
                                    } catch (Throwable t) {
                                        log.debug("Exception close vepJsonIterator", t);
                                    }
                                }
                            }
                        }).start();
                    } catch (Throwable t) {
                        log.debug("annotate Throwable", t);
                        o.tryOnError(t);
                    }
                })
        );
    }

}
