package org.forome.annotation.connector.gnomad;

import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GnomadConnector extends AutoCloseable {

    CompletableFuture<GnomadResult> request(String chromosome, long position, String reference, String alternative);

    List<DatabaseConnector.Metadata> getMetadata();

    void close();
}
