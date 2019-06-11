package org.forome.annotation.connector.gtf;

import org.forome.annotation.config.connector.GTFConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.gtf.struct.*;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    public CompletableFuture<List<GTFResultLookup>> getRegionByChromosomeAndPositions(String chromosome, long position1, long position2) {
        CompletableFuture<List<GTFResultLookup>> future = new CompletableFuture();
        threadPoolGTFExecutor.submit(() -> {
            try {
                List<GTFResultLookup> result =lookupByChromosomeAndPositions(chromosome, position1, position2);
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public Object[] lookup(long pos, String transcript) {
        List<GTFTranscriptRow> rows = gtfDataConnector.getTranscriptRows(transcript);
        if (rows.isEmpty()) return null;

        return lookup(pos, rows);
    }

    public List<GTFResultLookup> lookupByChromosomeAndPositions(String chromosome, long position1, long position2) {
        List<GTFTranscriptRowExternal> rows = gtfDataConnector.getTranscriptRowsByChromosomeAndPositions(chromosome, position1, position2);

        List<GTFResultLookup> result = new ArrayList<>();

        List<String> transcripts = rows.stream().map(row -> row.transcript).distinct().collect(Collectors.toList());
        for (String transcript: transcripts) {
            List<GTFTranscriptRow> transcriptRows = rows.stream().filter(row -> transcript.equals(row.transcript))
                    .collect(Collectors.toList());

            Object[] iResult1 = lookup(position1, transcriptRows);
            GTFRegion region1 = (GTFRegion)iResult1[1];
            result.add(new GTFResultLookup(transcript, position1, region1.region, region1.indexRegion));

            Object[] iResult2 = lookup(position2, transcriptRows);
            GTFRegion region2 = (GTFRegion)iResult2[1];
            result.add(new GTFResultLookup(transcript, position2, region2.region, region2.indexRegion));
        }

        return result;
    }

    public Object[] lookup(long pos, List<GTFTranscriptRow> rows) {
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
