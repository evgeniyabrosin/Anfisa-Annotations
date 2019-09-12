package org.forome.annotation.connector.gnomad.old;

import com.google.common.collect.ImmutableList;
import org.forome.annotation.config.connector.GnomadConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.struct.GnamadGroup;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;
import org.forome.annotation.matcher.SequenceMatcher;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GnomadConnectorOld implements AutoCloseable, GnomadConnector {

    private final static Logger log = LoggerFactory.getLogger(GnomadConnectorOld.class);

    private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private final DatabaseConnector databaseConnector;

    private final GnomadDataConnectorOld gnomadDataConnector;

    private final ExecutorService threadPoolGnomadExecutor;

    public GnomadConnectorOld(
            DatabaseConnectService databaseConnectService,
            GnomadConfigConnector gnomadConfigConnector,
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler
    ) throws Exception {
        databaseConnector = new DatabaseConnector(databaseConnectService, gnomadConfigConnector);
        gnomadDataConnector = new GnomadDataConnectorOld(databaseConnector);
        threadPoolGnomadExecutor = new DefaultThreadPoolExecutor(
                MAX_THREAD_COUNT,
                MAX_THREAD_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                "GnomadExecutorQueue",
                uncaughtExceptionHandler
        );
    }

    @Override
    public List<DatabaseConnector.Metadata> getMetadata() {
        return gnomadDataConnector.getMetadata();
    }

    public CompletableFuture<GnomadResult> request(String chromosome, long position, String reference, String alternative) {
        CompletableFuture<GnomadResult> future = new CompletableFuture();
        threadPoolGnomadExecutor.submit(() -> {
            try {
                GnomadResult result = syncRequest(chromosome, position, reference, alternative);
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private GnomadResult syncRequest(String chromosome, long position, String reference, String alternative) throws Exception {
        List<GnomadDataConnectorOld.Result> exomes = gnomadDataConnector.getData(
                chromosome, position, reference, alternative, "e", false
        );
        List<GnomadDataConnectorOld.Result> genomes = gnomadDataConnector.getData(
                chromosome, position, reference, alternative, "g", false
        );

        List<GnomadDataConnectorOld.Result> overall = new ImmutableList.Builder().addAll(exomes).addAll(genomes).build();
        if (overall.isEmpty()) {
            return null;
        }

        GnomadResult.Sum sumExomes = null;
        if (!exomes.isEmpty()) {
            long an = countAN(exomes, null);
            long ac = countAC(exomes, null);
            double af = countAF(an, ac);
            long hom = countHom(exomes);
            Long hem = countHem(chromosome, exomes);
            sumExomes = new GnomadResult.Sum(an, ac, af, hom, hem);
        }

        GnomadResult.Sum sumGenomes = null;
        if (!genomes.isEmpty()) {
            long an = countAN(genomes, null);
            long ac = countAC(genomes, null);
            double af = countAF(an, ac);
            long hom = countHom(genomes);
            Long hem = countHem(chromosome, genomes);
            sumGenomes = new GnomadResult.Sum(an, ac, af, hom, hem);
        }

        GnomadResult.Sum sumOverall = null;
        if (!overall.isEmpty()) {
            long an = countAN(overall, null);
            long ac = countAC(overall, null);
            double af = countAF(an, ac);
            long hom = countHom(overall);
            Long hem = countHem(chromosome, overall);
            sumOverall = new GnomadResult.Sum(an, ac, af, hom, hem);
        }

        Object[] popmaxFromRows = countPopmaxFromRows(overall);
        String popmax = (String) popmaxFromRows[0];
        double popmaxAF = (double) popmaxFromRows[1];
        long popmaxAN = (long) popmaxFromRows[2];

        Set<GnomadResult.Url> urls = new HashSet<>();
        for (GnomadDataConnectorOld.Result item : overall) {
            String chrom = (String) item.columns.get("CHROM");
            long pos = ((Number) item.columns.get("POS")).longValue();
            String ref = (String) item.columns.get("REF");
            String alt = (String) item.columns.get("ALT");

            SequenceMatcher matcher = new SequenceMatcher(ref, alt);
            List<SequenceMatcher.Tuple3<Integer, Integer, Integer>> matches = matcher.getMatchingBlocks();
            List<String> a = new ArrayList<>();
            List<String> b = new ArrayList<>();
            int apos = 0;
            int bpos = 0;
            for (SequenceMatcher.Tuple3<Integer, Integer, Integer> match : matches) {
                a.add(ref.substring(apos, match.value0));
                apos = match.value0 + match.value2;
                b.add(alt.substring(bpos, match.value1));
                bpos = match.value1 + match.value2;
            }

            String newRef = String.join("", a);
            String newAlt = String.join("", b);

            if (newRef.isEmpty() || newAlt.isEmpty()) {
                String firstMatch = String.valueOf(ref.charAt(0));
                newRef = firstMatch + newRef;
                newAlt = firstMatch + newAlt;
            }

            urls.add(new GnomadResult.Url(chrom, pos, newRef, newAlt));
        }

        return new GnomadResult(
                sumExomes, sumGenomes, sumOverall,
                null, new GnomadResult.Popmax(GnamadGroup.valueOf(popmax), popmaxAF, popmaxAN),
                urls
        );
    }

    @Override
    public void close() {
        databaseConnector.close();
    }

    private static long countAN(List<GnomadDataConnectorOld.Result> items, String group) {
        long an = 0;
        String anColumn;
        if (group == null) {
            anColumn = "AN";
        } else {
            anColumn = "AN_" + group;
        }
        for (GnomadDataConnectorOld.Result item : items) {
            Number value = (Number) item.columns.get(anColumn);
            if (value == null) continue;
            an += value.longValue();
        }
        return an;
    }

    private static long countAC(List<GnomadDataConnectorOld.Result> items, String group) {
        long ac = 0;
        String acColumn;
        if (group == null) {
            acColumn = "AC";
        } else {
            acColumn = "AC_" + group;
        }
        for (GnomadDataConnectorOld.Result item : items) {
            Number value = (Number) item.columns.get(acColumn);
            if (value == null) continue;
            ac += value.longValue();
        }
        return ac;
    }

    private static double countAF(long an, long ac) {
        double af;
        if (an > 0) {
            af = (double) ac / (double) an;
        } else {
            af = 0;
        }
        return af;
    }

    private static Object[] countPopmaxFromRows(List<GnomadDataConnectorOld.Result> overall) {
        String popmax = null;
        Double popmaxAF = null;
        long popmaxAN = 0;

        for (String group : GnomadDataConnectorOld.ANCESTRIES) {
            long an = countAN(overall, group);
            long ac = countAC(overall, group);
            if (an == 0) {
                continue;
            }
            double af = (double) ac / (double) an;
            if (popmaxAF == null || af > popmaxAF) {
                popmax = group;
                popmaxAF = af;
                popmaxAN = an;
            }
        }

        return new Object[]{
                popmax, (popmaxAF == null) ? 0 : popmaxAF, popmaxAN
        };
    }

    private static long countHom(List<GnomadDataConnectorOld.Result> items) {
        long hom = 0;
        String column = "Hom";
        for (GnomadDataConnectorOld.Result item : items) {
            Number value = (Number) item.columns.get(column);
            if (value == null) continue;
            hom += value.longValue();
        }
        return hom;
    }

    private static Long countHem(String chromosome, List<GnomadDataConnectorOld.Result> items) {
        if ("X".equals(chromosome.toUpperCase())) {
            return countAC(items, "Male");
        }
        return null;
    }
}
