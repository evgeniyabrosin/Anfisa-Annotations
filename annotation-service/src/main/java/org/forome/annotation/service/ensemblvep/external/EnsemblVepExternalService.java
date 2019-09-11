package org.forome.annotation.service.ensemblvep.external;

import net.minidev.json.JSONObject;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.variant.Variant;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class EnsemblVepExternalService implements EnsemblVepService {

    private final EnsemblVepHttpClient ensemblVepHttpClient;

    public EnsemblVepExternalService(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) throws IOException {
        this.ensemblVepHttpClient = new EnsemblVepHttpClient(uncaughtExceptionHandler);
    }

    @Override
    public CompletableFuture<JSONObject> getVepJson(Variant variant, String alternative) {
        String region = String.format("%s:%s:%s", variant.chromosome.getChar(), variant.start, variant.end);
        String endpoint = String.format("/vep/human/region/%s/%s?hgvs=true&canonical=true&merged=true&protein=true&variant_class=true", region, alternative);
        return ensemblVepHttpClient.request(endpoint).thenApply(jsonArray -> (JSONObject) jsonArray.get(0));
    }

    @Override
    public CompletableFuture<JSONObject> getVepJson(Variant variant, String reference, String alternative) {
        return getVepJson(variant, alternative);
    }

    @Override
    public void close() {
        ensemblVepHttpClient.close();
    }
}
