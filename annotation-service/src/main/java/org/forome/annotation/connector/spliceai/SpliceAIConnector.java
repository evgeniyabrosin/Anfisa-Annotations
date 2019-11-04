package org.forome.annotation.connector.spliceai;

import org.forome.annotation.config.connector.SpliceAIConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Allele;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpliceAIConnector implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(SpliceAIConnector.class);

    public final static float MAX_DS_UNLIKELY = 0.2f;

    private final DatabaseConnector databaseConnector;

    private final SpliceAIDataConnector spliceAIDataConnector;

    public SpliceAIConnector(
            DatabaseConnectService databaseConnectService,
            SpliceAIConfigConnector spliceAIConfigConnector
    ) throws Exception {
        databaseConnector = new DatabaseConnector(databaseConnectService, spliceAIConfigConnector);
        spliceAIDataConnector = new SpliceAIDataConnector(databaseConnector);
    }

    public List<DatabaseConnector.Metadata> getMetadata(){
        return databaseConnector.getMetadata();
    }

    public SpliceAIResult getAll(String chromosome, long position, String ref, Allele altAllele){
        return spliceAIDataConnector.getAll(chromosome, position, ref, altAllele);
    }

    @Override
    public void close() {
        databaseConnector.close();
    }

}
