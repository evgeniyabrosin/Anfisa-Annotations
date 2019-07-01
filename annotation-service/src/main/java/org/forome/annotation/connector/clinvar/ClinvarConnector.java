package org.forome.annotation.connector.clinvar;

import org.forome.annotation.config.connector.ClinVarConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.clinvar.struct.ClinvarResult;
import org.forome.annotation.connector.clinvar.struct.Row;
import org.forome.annotation.exception.ExceptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ClinvarConnector implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(ClinvarConnector.class);

	private static final String SUBMITTER_QUERY = "SELECT SubmitterName, ClinicalSignificance FROM `clinvar`.`CV_Submitters_A` NATURAL JOIN `clinvar`.`ClinVar2Sub_Sig_A` WHERE RCVaccession IN (%s)";

	private static final String QUERY_BASE = "SELECT " +
			"`Start`," +
			"`Stop`," +
			"`AlternateAllele`," +
			"`Type`," +
			"ClinicalSignificance," +
			"PhenotypeIDS," +
			"PhenotypeList," +
			"OtherIDs, " +
			"RCVaccession, " +
			"ReferenceAllele, " +
			"VariationID " +
			"FROM clinvar.variant_summary AS v " +
			"WHERE " +
			"Assembly = 'GRCh37' AND " +
			"Chromosome='%s' AND " +
			"Start = %s ";

	private static final String QUERY_0 = QUERY_BASE + " AND Stop = %s ";
	private static final String QUERY = QUERY_0 + "AND (AlternateAllele = %s OR AlternateAllele = 'na')";
	private static final String QUERY_EXACT = QUERY_0 + " AND AlternateAllele = '%s'";
	private static final String QUERY_NA = QUERY_0 + " AND AlternateAllele = 'na'";

	private final DatabaseConnector databaseConnector;

	public ClinvarConnector(ClinVarConfigConnector clinVarConfigConnector) throws Exception {
		this.databaseConnector = new DatabaseConnector(clinVarConfigConnector);
	}

	private ClinvarResult getSubmitters(Row row) {
		String[] rcvAccessions = row.rcvAccession.split(";");
		String args = String.join(",", Stream.of(rcvAccessions).map(s -> "'" + s + "'").collect(Collectors.toList()));

		Map<String, String> submitters = new HashMap<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(String.format(SUBMITTER_QUERY, args))) {
					while (resultSet.next()) {
						submitters.put(resultSet.getString(1), resultSet.getString(2));
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}

		return new ClinvarResult(
				row.start, row.end,
				row.referenceAllele, row.alternateAllele,
				row.variationID, row.clinicalSignificance,
				row.phenotypeIDs, row.otherIDs,
				row.phenotypeList,
				submitters
		);
	}

	private List<ClinvarResult> addSubmittersToRows(List<Row> rows) {
		List<ClinvarResult> results = new ArrayList<>();
		for (Row row : rows) {
			results.add(getSubmitters(row));
		}
		return results;
	}


	public List<ClinvarResult> getExpandedData(String chromosome, long qStart) {
		List<Row> rows = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(String.format(QUERY_BASE, chromosome, qStart))) {
					while (resultSet.next()) {
						rows.add(_build(resultSet));
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return addSubmittersToRows(rows);
	}

	public List<ClinvarResult> getData(String chromosome, long qStart, long qEnd, List<String> alts) {
        List<Row> rows = new ArrayList<>();
	    try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String alt: alts) {
                    try (ResultSet resultSet = statement.executeQuery(String.format(QUERY_EXACT, chromosome, qStart, qEnd, alt))) {
                        while (resultSet.next()) {
                            rows.add(_build(resultSet));
                        }
                    }
                }

                if (rows.isEmpty()) {
                    try (ResultSet resultSet = statement.executeQuery(String.format(QUERY_NA, chromosome, qStart, qEnd))) {
                        while (resultSet.next()) {
                            rows.add(_build(resultSet));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return addSubmittersToRows(rows);
	}

	private static Row _build(ResultSet resultSet) throws SQLException {
        long start = resultSet.getLong("Start");
        long end = resultSet.getLong("Stop");
        String referenceAllele = resultSet.getString("ReferenceAllele");
        String alternateAllele = resultSet.getString("AlternateAllele");
        String rcvAccession = resultSet.getString("RCVaccession");
        String variationID = resultSet.getString("VariationID");
        String clinicalSignificance = resultSet.getString("ClinicalSignificance");
        String phenotypeIDs = resultSet.getString("PhenotypeIDS");
        String otherIDs = resultSet.getString("OtherIDs");
        String phenotypeList = resultSet.getString("PhenotypeList");
        return new Row(
                start, end,
                referenceAllele, alternateAllele,
                rcvAccession, variationID, clinicalSignificance,
                phenotypeIDs, otherIDs,
                phenotypeList
        );
    }

	@Override
	public void close() {
		databaseConnector.close();
	}
}
