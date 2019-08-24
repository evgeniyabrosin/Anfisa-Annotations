package org.forome.annotation.service.ensemblvep.inline.struct;

import net.minidev.json.JSONObject;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class EnsembleVepRequest {

    public final String request;
    public final CompletableFuture<JSONObject> future;

    private Instant time;

    public EnsembleVepRequest(String request, CompletableFuture<JSONObject> future) {
        this.request = request;
        this.future = future;

        time = Instant.now();
    }

    public Instant getTime() {
        return time;
    }

    public void touch() {
        time = Instant.now();
    }
}
