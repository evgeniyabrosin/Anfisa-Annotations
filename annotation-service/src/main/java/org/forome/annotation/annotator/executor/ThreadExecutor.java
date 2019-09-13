package org.forome.annotation.annotator.executor;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONObject;
import org.forome.annotation.annotator.input.FileReaderIterator;
import org.forome.annotation.annotator.input.VCFFileIterator;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaInput;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.sample.Samples;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantVCF;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ThreadExecutor implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(ThreadExecutor.class);

    private final int index;

    private final EnsemblVepService ensemblVepService;
    private final AnfisaConnector anfisaConnector;

    private final String caseSequence;
    private final Samples samples;

    private final int start;
    private final int step;

    private final VCFFileIterator vcfFileIterator;
    private final FileReaderIterator vepJsonIterator;

    private Result nextResult;
    private final Deque<Result> waitExecuteVariants;//Варианты ожидающие выполнения

    private int nextPosition;
    private volatile boolean isCompleted = false;

    public ThreadExecutor(
            int index,
            EnsemblVepService ensemblVepService,
            AnfisaConnector anfisaConnector,
            String caseSequence, Samples samples,
            Path pathVcf, Path pathVepJson,
            Path cnvFile,
            int start, int step,
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        this.index = index;

        this.ensemblVepService = ensemblVepService;
        this.anfisaConnector = anfisaConnector;

        this.caseSequence = caseSequence;
        this.samples = samples;

        this.start = start;
        this.step = step;

        this.vcfFileIterator = new VCFFileIterator(pathVcf, cnvFile);

        if (pathVepJson != null) {
            vepJsonIterator = new FileReaderIterator(pathVepJson);
        } else {
            vepJsonIterator = null;
        }

        this.nextResult = new Result(nextPosition, new CompletableFuture<>());
        this.waitExecuteVariants = new ConcurrentLinkedDeque<>();
        this.waitExecuteVariants.add(nextResult);

        nextPosition = start + step;

        //Исполнитель
        Thread executor = new Thread(() -> {
            log.debug("Thread: {} start", index);

            Source source = null;
            try {
                //Прокручиваем до начала итерации
                if (start > 0) {
                    nextSource(start);
                }
                source = nextSource(1);
            } catch (NoSuchElementException e) {
                isCompleted = true;
                log.debug("Thread: {} completed", index);
            }

            while (true) {

                //TODO Переписать на засыпание потока
                while (waitExecuteVariants.isEmpty()) {
                    if (isCompleted) return;
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                    }
                }
                Result result = waitExecuteVariants.poll();
                if (isCompleted) {
                    result.future.complete(null);
                    continue;
                }

                Variant variant = source.variant;
                JSONObject vepJson = source.getVepJson();

                if (variant instanceof VariantVCF && vepJsonIterator != null) {
                    int iStart = vepJson.getAsNumber("start").intValue();
                    int iEnd = vepJson.getAsNumber("end").intValue();

                    AnfisaInput anfisaInput = new AnfisaInput.Builder()
                            .withSamples(samples)
                            .build();

                    AnfisaResult anfisaResult = anfisaConnector.build(
                            caseSequence,
                            anfisaInput,
                            new VariantVCF(((VariantVCF) variant).variantContext, iStart, iEnd),
                            vepJson
                    );
                    result.future.complete(anfisaResult);
                } else {
                    String alternative;
                    if (variant instanceof VariantVCF) {
                        VariantContext variantContext = ((VariantVCF) variant).variantContext;

                        Allele allele = variantContext.getAlternateAlleles().stream()
                                .filter(iAllele -> !iAllele.getDisplayString().equals("*"))
                                .max(Comparator.comparing(variantContext::getCalledChrCount))
                                .orElse(null);
                        alternative = allele.getDisplayString();
                    } else if (variant instanceof VariantCNV) {
                        alternative = "-";
                    } else {
                        throw new RuntimeException("Not support type variant: " + variant);
                    }

                    ensemblVepService.getVepJson(variant, alternative)
                            .thenApply(iVepJson -> {
                                AnfisaInput anfisaInput = new AnfisaInput.Builder()
                                        .withSamples(samples)
                                        .build();
                                AnfisaResult anfisaResult = anfisaConnector.build(null, anfisaInput, variant, iVepJson);
                                result.future.complete(anfisaResult);
                                return null;
                            })
                            .exceptionally(throwable -> {
                                result.future.completeExceptionally(throwable);
                                return null;
                            });
                }

                //Дожидаемся выполнения
                try {
                    result.future.join();
                } catch (Throwable ignore) {
                }

                try {
                    source = nextSource(step);
                } catch (NoSuchElementException e) {
                    isCompleted = true;
                    log.debug("Thread: {} completed", index);
                }
            }
        });
        executor.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        executor.start();
    }

    private Source nextSource(int step) {
        if (step < 1) throw new IllegalArgumentException();
        Variant variant = null;
        String strVepJson = null;
        for (int i = 0; i < step; i++) {
            try {
                variant = vcfFileIterator.next();
            } catch (NoSuchElementException ne) {
                //Валидация того, что в vep.json - тоже не осталось записей
                if (vepJsonIterator != null) {
                    try {
                        vepJsonIterator.next();
                        throw new RuntimeException("Not equals count rows, vcf file and vep.json file");
                    } catch (NoSuchElementException ignore) {
                    }
                }
                throw ne;
            }
            if (variant instanceof VariantVCF && vepJsonIterator != null) {
                try {
                    strVepJson = vepJsonIterator.next();
                } catch (NoSuchElementException ne) {
                    //Валидация того, что в vep.json - остались записи
                    throw new RuntimeException("Not equals count rows, vcf file and vep.json file");
                }
            } else {
                strVepJson = null;
            }
        }
        return new Source(variant, strVepJson);
    }

    public Result next() {
        Result value;
        synchronized (waitExecuteVariants) {
            value = nextResult;

            nextPosition += step;
            if (isCompleted) {
                nextResult = new Result(nextPosition, CompletableFuture.completedFuture(null));
            } else {
                nextResult = new Result(nextPosition, new CompletableFuture<>());
                this.waitExecuteVariants.add(nextResult);
            }
        }
        return value;
    }


    @Override
    public void close() throws IOException {
        vcfFileIterator.close();

        if (vepJsonIterator != null) {
            vepJsonIterator.close();
        }
    }
}
