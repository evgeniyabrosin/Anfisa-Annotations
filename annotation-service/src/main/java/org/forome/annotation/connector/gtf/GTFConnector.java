package org.forome.annotation.connector.gtf;

import org.forome.annotation.config.connector.GTFConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.gtf.struct.GTFRegion;
import org.forome.annotation.connector.gtf.struct.GTFResult;
import org.forome.annotation.connector.gtf.struct.GTFTranscriptRow;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GTFConnector {

    private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private final DatabaseConnector databaseConnector;
    private final GTFDataConnector gtfDataConnector;

    private final ExecutorService threadPoolGTFExecutor;

    public GTFConnector(
            GTFConfigConnector gtfConfigConnector,
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler
    ) throws Exception {
        this.databaseConnector = new DatabaseConnector(gtfConfigConnector);
        this.gtfDataConnector = new GTFDataConnector(databaseConnector);
        threadPoolGTFExecutor = new DefaultThreadPoolExecutor(
                MAX_THREAD_COUNT,
                MAX_THREAD_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                "GnomadExecutorQueue",
                uncaughtExceptionHandler
        );
    }

    public CompletableFuture<GTFResult> request(String chromosome, long position) {
        CompletableFuture<GTFResult> future = new CompletableFuture();
        threadPoolGTFExecutor.submit(() -> {
            try {
                GTFResult result = gtfDataConnector.getGene(chromosome, position);
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<GTFRegion> getRegion(String transcript, long position) {
        CompletableFuture<GTFRegion> future = new CompletableFuture();
        threadPoolGTFExecutor.submit(() -> {
            try {
                Object[] result = lookup(position, transcript);
                future.complete((GTFRegion) result[1]);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public Object[] lookup(long pos, String transcript) {
        List<GTFTranscriptRow> rows = gtfDataConnector.getTranscriptRows(transcript);
        if (rows.isEmpty()) return null;

        long inf = rows.get(0).start;
        if (pos < inf) {
            return new Object[]{(inf - pos), GTFRegion.UPSTREAM};
        }

        long sup = rows.get(rows.size() - 1).end;
        if (pos > sup) {
            return new Object[]{(pos - sup), GTFRegion.DOWNSTREAM};
        }

        List<Long> a = new ArrayList<>();
        for (GTFTranscriptRow row : rows) {
            a.add(row.start);
            a.add(row.end);
        }

        //Аналог: i = bisect.bisect(a, pos)
        int lo = 0;
        int hi = a.size();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (pos < a.get(mid)) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        int i = lo;

        long d;
        if (pos == inf || pos == sup) {
            d = 0;
        } else {
            d = Math.min(pos - a.get(i - 1), a.get(i) - pos);
        }

        long index;
        String region;
        if ((i % 2) == 1) {
            index = (i + 1) / 2;
            region = "exon";
        } else {
            index = i / 2;
            region = "intron";
        }

        return new Object[]{d, new GTFRegion(region, index), rows.size()};
    }


}
