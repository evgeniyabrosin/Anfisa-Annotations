package org.forome.annotation.annotator.executor;

import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.mcase.MCase;

import java.io.IOException;
import java.nio.file.Path;

public class AnnotatorExecutor implements AutoCloseable {

    private final ThreadExecutor[] threadExecutors;

    private int activeExecutor;

    public AnnotatorExecutor(
            EnsemblVepService ensemblVepService,
            AnfisaConnector anfisaConnector,
            String caseSequence, MCase samples,
            Path pathVepVcf, Path pathVepJson,
            Path cnvFile,
            int start, int thread,
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        if (thread < 1) throw new IllegalArgumentException();

        threadExecutors = new ThreadExecutor[thread];
        for (int i = 0; i < thread; i++) {
            threadExecutors[i] = new ThreadExecutor(
                    i + 1,
                    ensemblVepService,
                    anfisaConnector,
                    caseSequence, samples,
                    pathVepVcf, pathVepJson,
                    cnvFile,
                    start + i, thread,
                    uncaughtExceptionHandler
            );
        }

        activeExecutor = 0;
    }

    public synchronized Result next() {
        ThreadExecutor threadExecutor = threadExecutors[activeExecutor];
        activeExecutor++;
        if (activeExecutor > threadExecutors.length - 1) {
            activeExecutor = 0;
        }

        return threadExecutor.next();
    }

    @Override
    public void close() throws IOException {
        for (ThreadExecutor threadExecutor: threadExecutors) {
            threadExecutor.close();
        }
    }
}
