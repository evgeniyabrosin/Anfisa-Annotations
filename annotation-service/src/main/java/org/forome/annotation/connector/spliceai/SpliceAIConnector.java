package org.forome.annotation.connector.spliceai;

import org.forome.annotation.config.connector.SpliceAIConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpliceAIConnector implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(SpliceAIConnector.class);

    public final static float MAX_DS_UNLIKELY = 0.2f;

    private final DatabaseConnector databaseConnector;

    private final SpliceAIDataConnector spliceAIDataConnector;


    public SpliceAIConnector(
            SpliceAIConfigConnector spliceAIConfigConnector,
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler
    ) throws Exception {
        databaseConnector = new DatabaseConnector(spliceAIConfigConnector);
        spliceAIDataConnector = new SpliceAIDataConnector(databaseConnector);
    }

    public SpliceAIResult getAll(String chromosome, long position, String ref, List<String> altList){
        return spliceAIDataConnector.getAll(chromosome, position, ref, altList);
    }

    public String getSpliceAIDataVersion() {
        return spliceAIDataConnector.getSpliceAIDataVersion();
    }

    @Override
    public void close() {
        databaseConnector.close();
    }

}
