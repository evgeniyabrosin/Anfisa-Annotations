package org.forome.annotation.connector.ref;

import org.forome.annotation.config.connector.RefConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

public class RefConnector implements AutoCloseable {

    private final DatabaseConnector databaseConnector;
    private final RefDataConnector refDataConnector;

    public RefConnector(
            DatabaseConnectService databaseConnectService,
            RefConfigConnector refConfigConnector
    ) throws Exception {
        this.databaseConnector = new DatabaseConnector(databaseConnectService, refConfigConnector);
        this.refDataConnector = new RefDataConnector(databaseConnector);
    }

    public String getRef(Variant variant) {
        return refDataConnector.getRef(variant.chromosome, variant.getStart(), variant.end);
    }

    public String getRef(Chromosome chromosome, int start, int end) {
        return refDataConnector.getRef(chromosome, start, end);
    }

    @Override
    public void close() {
        databaseConnector.close();
    }
}
