/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.data.gtf.datasource.mysql;

import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.gtf.datasource.GTFDataSource;
import org.forome.annotation.data.gtf.mysql.struct.GTFResult;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRowExternal;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Position;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GTFDataConnector implements GTFDataSource {

    private static final long GENE_BUCKET_SIZE = 1000000L;

    private final DatabaseConnector databaseConnector;

    public GTFDataConnector(DatabaseConnector databaseConnector) {
        this.databaseConnector = databaseConnector;
    }

    public GTFResult getGene(String chromosome, long position) {
        long bucket = (position / GENE_BUCKET_SIZE) * GENE_BUCKET_SIZE;

        String sql = String.format(
                "SELECT gene FROM ensembl.GTF_gene WHERE chromosome = %s AND bucket = %s AND %s between `start` and `end`",
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
                "SELECT `gene`, `start`, `end`, `feature` from ensembl.GTF WHERE transcript = '%s' AND feature = 'exon' ORDER BY `start`, `end`",
                transcript
        );

        List<GTFTranscriptRow> rows = new ArrayList<GTFTranscriptRow>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String gene = resultSet.getString("gene");
                        int start = resultSet.getInt("start");
                        int end = resultSet.getInt("end");
                        String feature = resultSet.getString("feature");
                        rows.add(new GTFTranscriptRow(
                                gene,
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

    public List<GTFTranscriptRowExternal> getTranscriptRowsByChromosomeAndPositions(String chromosome, long[] positions) {

        String sqlWherePosition = Arrays.stream(positions)
                .mapToObj(position-> String.format("(`start` < %s and %s < `end`)", position, position))
                .collect(Collectors.joining(" or ", "(", ")"));

        String sql = String.format(
                "SELECT `transcript`, `gene`, `approved`, `start`, `end`, `feature` from ensembl.GTF WHERE feature IN ('transcript') and chromosome = '%s' and %s" +
                        " ORDER BY `start`, `end`",
                chromosome, sqlWherePosition
        );

        List<GTFTranscriptRowExternal> rows = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String transcript = resultSet.getString("transcript");
                        String gene = resultSet.getString("gene");
                        String approved = resultSet.getString("approved");
                        int start = resultSet.getInt("start");
                        int end = resultSet.getInt("end");
                        String feature = resultSet.getString("feature");
                        rows.add(new GTFTranscriptRowExternal(
                                transcript, gene, approved,
                                start, end, feature
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return rows;
    }


    public List<String> getTranscriptsByChromosomeAndPositions(String chromosome, long[] positions) {
        String sqlWherePosition = Arrays.stream(positions)
                .mapToObj(position-> String.format("(`start` < %s and %s < `end`)", position, position))
                .collect(Collectors.joining(" or ", "(", ")"));

        String sql = String.format(
                "SELECT `transcript` from ensembl.GTF WHERE feature IN ('transcript') and chromosome = '%s' and %s" +
                        " ORDER BY `start`, `end`",
                chromosome, sqlWherePosition
        );

        List<String> transcripts = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String transcript = resultSet.getString("transcript");

                        if (!transcripts.contains(transcript)) {
                            transcripts.add(transcript);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return transcripts;
    }

	@Override
	public List<GTFTranscriptRow> lookup(Assembly assembly, Position position, String transcript) {
		throw new RuntimeException("Not implemented");
	}

	@Override
    public void close() {
        databaseConnector.close();
    }
}
