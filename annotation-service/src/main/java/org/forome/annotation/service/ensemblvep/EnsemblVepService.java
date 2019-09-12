package org.forome.annotation.service.ensemblvep;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.variant.Variant;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface EnsemblVepService extends Closeable {

    CompletableFuture<JSONObject> getVepJson(Variant variant, String alternative);

    CompletableFuture<JSONObject> getVepJson(Variant variant, String reference, String alternative);

    void close();

}
