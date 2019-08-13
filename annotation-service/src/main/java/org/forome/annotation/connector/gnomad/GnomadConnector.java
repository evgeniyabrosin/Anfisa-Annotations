package org.forome.annotation.connector.gnomad;

import org.forome.annotation.connector.gnomad.struct.GnomadResult;

import java.util.concurrent.CompletableFuture;

public interface GnomadConnector extends AutoCloseable {

    public CompletableFuture<GnomadResult> request(String chromosome, long position, String reference, String alternative);

    void close();
}
