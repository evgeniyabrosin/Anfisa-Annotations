package org.forome.annotation.annotator.executor;

import org.forome.annotation.connector.anfisa.struct.AnfisaResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Result {

    public final int position;
    public final CompletableFuture<List<AnfisaResult>> future;

    public Result(int position, CompletableFuture<List<AnfisaResult>> future) {
        this.position = position;
        this.future = future;
    }
}
