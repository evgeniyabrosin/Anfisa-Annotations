package org.forome.annotation.annotator.executor;

import org.forome.annotation.connector.anfisa.struct.AnfisaResult;

import java.util.concurrent.CompletableFuture;

public class Result {

    public final int position;
    public final CompletableFuture<AnfisaResult> future;

    public Result(int position, CompletableFuture<AnfisaResult> future) {
        this.position = position;
        this.future = future;
    }
}
