package org.forome.annotation.annotator;

import io.reactivex.Observable;
import org.forome.annotation.annotator.executor.AnnotatorExecutor;
import org.forome.annotation.annotator.executor.Result;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.annotator.utils.CaseUtils;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.struct.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Annotator {

    private final static Logger log = LoggerFactory.getLogger(Annotator.class);

    private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 4;

    private final AnfisaConnector anfisaConnector;

    public Annotator(AnfisaConnector anfisaConnector) {
        this.anfisaConnector = anfisaConnector;
    }

    public AnnotatorResult exec(
            String caseName,
            Path pathFam,
            Path pathVepVcf,
            Path pathVepJson,
            int startPosition
    ) throws IOException {
        if (!Files.exists(pathFam)) {
            throw new RuntimeException("Fam file is not exists: " + pathFam.toAbsolutePath());
        }
        if (!pathFam.getFileName().toString().endsWith(".fam")) {
            throw new IllegalArgumentException("Bad name fam file: " + pathFam.toAbsolutePath());
        }

        if (!Files.exists(pathVepVcf)) {
            throw new RuntimeException("Vcf file is not exists: " + pathVepVcf.toAbsolutePath());
        }
        if (!pathVepVcf.getFileName().toString().endsWith(".vcf")) {
            throw new IllegalArgumentException("Bad name vcf file (Need *.vcf): " + pathVepVcf.toAbsolutePath());
        }

        if (pathVepJson != null) {
            if (!Files.exists(pathVepJson)) {
                throw new RuntimeException("VepJson file is not exists: " + pathVepJson.toAbsolutePath());
            }
            String vepJsonFileName = pathVepJson.getFileName().toString();
            if (!(vepJsonFileName.endsWith(".json") || vepJsonFileName.endsWith(".json.gz"))) {
                throw new IllegalArgumentException("Bad name pathVepJson file (Need *.json vs *.json.gz): " + pathVepJson.toAbsolutePath());
            }
        }

        try (InputStream isFam = Files.newInputStream(pathFam)) {
            return exec(
                    caseName,
                    isFam,
                    pathVepVcf,
                    pathVepJson,
                    startPosition
            );
        }
    }

    public AnnotatorResult exec(
            String caseName,
            InputStream isFam,
            Path pathVepVcf,
            Path pathVepJson,
            int startPosition
    ) throws IOException {

        Map<String, Sample> samples = CaseUtils.parseFamFile(isFam);

        String platform;
        Set<String> x = Arrays.stream(pathVepVcf.getFileName().toString().toLowerCase().split("_"))
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
                caseId, samples,
                pathVepVcf, pathVepJson,
                startPosition
        );
    }

    public AnnotatorResult annotateJson(
            String caseSequence, Map<String, Sample> samples,
            Path pathVepVcf, Path pathVepJson,
            int startPosition
    ) {
        return new AnnotatorResult(
                AnnotatorResult.Metadata.build(caseSequence, pathVepVcf, samples),
                Observable.create(o -> {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try (AnnotatorExecutor annotatorExecutor = new AnnotatorExecutor(
                                    anfisaConnector,
                                    caseSequence, samples,
                                    pathVepVcf, pathVepJson,
                                    startPosition, MAX_THREAD_COUNT)
                            ) {
                                boolean run = true;
                                while (run) {
                                    Result result = annotatorExecutor.next();
                                    AnfisaResult anfisaResult;
                                    try {
                                        anfisaResult = result.future.get();
                                        if (anfisaResult == null) {
                                            run = false;
                                        }
                                        o.onNext(anfisaResult);
                                    } catch (Throwable e) {
                                        log.error("throwable", e);
                                    }
                                }
                                o.onComplete();
                            } catch (Throwable e) {
                                o.tryOnError(e);
                            }
                        }
                    }).start();
                })
        );
    }

}
