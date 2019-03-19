package org.forome.annotation.connector.gtf;

import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.gtf.struct.GTFResult;
import org.forome.annotation.connector.gtf.struct.GTFTranscriptRow;
import org.forome.annotation.exception.ExceptionBuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GTFDataConnector {

    private static final long GENE_BUCKET_SIZE = 1000000L;

    private final DatabaseConnector databaseConnector;

    public GTFDataConnector(DatabaseConnector databaseConnector) {
        this.databaseConnector = databaseConnector;
    }

    public GTFResult getGene(String chromosome, long position) {
        long bucket = (position / GENE_BUCKET_SIZE) * GENE_BUCKET_SIZE;

        String sql = String.format(
                "SELECT gene FROM GTF_gene WHERE chromosome = %s AND bucket = %s AND %s between `start` and `end`",
                chromosome, bucket, position
        );

        String symbol = null;
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    if (resultSet.next()) {
                        symbol = resultSet.getString(1);
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return new GTFResult(symbol);
    }

    public List<GTFTranscriptRow> getTranscriptRows(String transcript) {
        String sql = String.format(
                "SELECT `start`, `end`, `feature` from ensembl.GTF WHERE transcript = '%s' AND feature = 'exon' ORDER BY `start`, `end`",
                transcript
        );

        List<GTFTranscriptRow> rows = new ArrayList<GTFTranscriptRow>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        long start = resultSet.getLong("start");
                        long end = resultSet.getLong("end");
                        String feature = resultSet.getString("feature");
                        rows.add(new GTFTranscriptRow(
                                start,
                                end,
                                feature
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return rows;
    }
}
