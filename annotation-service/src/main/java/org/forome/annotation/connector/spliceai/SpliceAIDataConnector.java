package org.forome.annotation.connector.spliceai;

import com.google.common.collect.ImmutableList;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.spliceai.struct.Row;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.forome.annotation.exception.ExceptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpliceAIDataConnector {

    private static final Logger log = LoggerFactory.getLogger(SpliceAIDataConnector.class);

    private final DatabaseConnector databaseConnector;

    private static final String TABLE = "spliceai.SPLICEAI";

    public static final ImmutableList<String> KEY_COLUMNS = ImmutableList.of(
            "CHROM",
            "POS",
            "REF",
            "ALT"
    );

    public static final ImmutableList<String> TARGET_COLUMNS = ImmutableList.of(
            "SYMBOL",
            "STRAND",
            "TYPE",
            "DP_AG",
            "DP_AL",
            "DP_DG",
            "DP_DL",
            "DS_AG",
            "DS_AL",
            "DS_DG",
            "DS_DL",
            "ID",
            "MAX_DS"
    );

    private static final ImmutableList<String> COLUMNS =
            new ImmutableList.Builder()
                    .addAll(KEY_COLUMNS)
                    .addAll(TARGET_COLUMNS)
                    .build();


    public SpliceAIDataConnector(DatabaseConnector databaseConnector) {
        this.databaseConnector = databaseConnector;
    }

    public SpliceAIResult getAll(String chromosome, long position, String ref, List<String> alt_list) {
        if (alt_list.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String select_list = String.join(", ", COLUMNS);
        String alt_values = alt_list.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
        String sql = String.format("SELECT %s FROM %s WHERE CHROM = '%s' AND POS = %s AND REF = '%s' AND ALT IN (%s)",
                select_list, TABLE, chromosome, position, ref, alt_values
        );
        List<Row> rows = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        rows.add(_build(resultSet));
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }

        String cases;
        Float max_ds;
        Map<String, SpliceAIResult.DictSql> dict_sql = new HashMap<>();
        if (rows.isEmpty()) {
            cases = "None";
            max_ds = null;
        } else {
            max_ds = rows.stream().map(row -> row.max_ds).max(Float::compareTo).orElse(null);
            if (max_ds < SpliceAIConnector.MAX_DS_UNLIKELY) {
                cases = "unlikely";
            } else if (max_ds < 0.5f) {
                cases = "likely_pathogenic";
            } else if (max_ds < 0.8f) {
                cases = "pathogenic";
            } else if (max_ds <= 1.0f) {
                cases = "high_precision_pathogenic";
            } else {
                throw new RuntimeException("Not support value max_ds: " + max_ds);
            }
            for (Row row: rows) {
                dict_sql.put(
                        String.format("%s/%s/%s/%s", row.alt, row.symbol, row.strand, row.type),
                        new SpliceAIResult.DictSql(
                                row.dp_ag, row.dp_al, row.dp_dg, row.dp_dl,
                                row.ds_ag, row.ds_al, row.ds_dg, row.ds_dl
                        )
                );
            }
        }
        return new SpliceAIResult(cases, max_ds, dict_sql);
    }

    public String getSpliceAIDataVersion() {
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT version FROM VERSION")) {
                    resultSet.next();
                    return resultSet.getString(1);
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
    }

    private static Row _build(ResultSet resultSet) throws SQLException {
        String chrom = resultSet.getString("CHROM");
        int pos = resultSet.getInt("POS");
        String ref = resultSet.getString("REF");
        String alt = resultSet.getString("ALT");
        String symbol = resultSet.getString("SYMBOL");
        String strand = resultSet.getString("STRAND");
        String type = resultSet.getString("TYPE");
        int dp_ag = resultSet.getInt("DP_AG");
        int dp_al = resultSet.getInt("DP_AL");
        int dp_dg = resultSet.getInt("DP_DG");
        int dp_dl = resultSet.getInt("DP_DL");
        float ds_ag = resultSet.getFloat("DS_AG");
        float ds_al = resultSet.getFloat("DS_AL");
        float ds_dg = resultSet.getFloat("DS_DG");
        float ds_dl = resultSet.getFloat("DS_DL");
        String id = resultSet.getString("ID");
        float max_ds = resultSet.getFloat("MAX_DS");

        return new Row(
                chrom, pos, ref, alt,
                symbol, strand, type,
                dp_ag, dp_al, dp_dg, dp_dl,
                ds_ag, ds_al, ds_dg, ds_dl,
                id, max_ds
        );
    }
}
