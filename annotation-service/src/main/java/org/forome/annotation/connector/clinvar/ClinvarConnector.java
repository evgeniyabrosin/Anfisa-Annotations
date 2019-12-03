/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.connector.clinvar;

import org.forome.annotation.config.connector.ClinVarConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.clinvar.struct.ClinvarResult;
import org.forome.annotation.connector.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.connector.clinvar.struct.Row;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;
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
import java.util.stream.Stream;

public class ClinvarConnector implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(ClinvarConnector.class);

	private static final String SUBMITTER_QUERY = "SELECT SubmitterName, ClinicalSignificance FROM `clinvar`.`CV_Submitters` NATURAL JOIN `clinvar`.`ClinVar2Sub_Sig` WHERE RCVaccession IN (%s)";

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
	private static final String QUERY_EXACT = QUERY_0 + " AND AlternateAllele = '%s'";
	private static final String QUERY_NA = QUERY_0 + " AND AlternateAllele = 'na'";

	private static final String QUERY_VARIANT_SUMMARY =
			"select ReviewStatus, NumberSubmitters, Guidelines from clinvar.variant_summary where Chromosome='%s' AND Start = %s and Stop = %s";

	private static final String CLINVAR_TYPE_SNV = "single nucleotide variant";
	private static final String CLINVAR_TYPE_DELETION = "deletion";

	private final DatabaseConnector databaseConnector;

	public ClinvarConnector(DatabaseConnectService databaseConnectService, ClinVarConfigConnector clinVarConfigConnector) throws Exception {
		this.databaseConnector = new DatabaseConnector(databaseConnectService, clinVarConfigConnector);
	}

	public List<DatabaseConnector.Metadata> getMetadata() {
		return databaseConnector.getMetadata();
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


	public List<ClinvarResult> getExpandedData(Variant variant) {
		List<Row> rows = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(String.format(
						QUERY_BASE, variant.chromosome.getChar(), variant.getStart()
				))) {
					while (resultSet.next()) {
						Row row = _build(resultSet);
						//TODO Ulitin V. Необходим комплексный подход - сейчас проверяем только на SNV //и Deletion
						if (variant.getVariantType() != VariantType.INDEL && variant.getVariantType() != VariantType.SEQUENCE_ALTERATION) {
							if (CLINVAR_TYPE_SNV.equals(row.type) && variant.getVariantType() != VariantType.SNV) {
								continue;
							}
//						else if (CLINVAR_TYPE_DELETION.equals(row.type) && variant.getVariantType() != VariantType.DEL) {
//							continue;
//						}

						}

						rows.add(row);
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
				for (String alt : alts) {
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
		String type = resultSet.getString("Type");
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
				type,
				referenceAllele, alternateAllele,
				rcvAccession, variationID, clinicalSignificance,
				phenotypeIDs, otherIDs,
				phenotypeList
		);
	}

	public ClinvarVariantSummary getDataVariantSummary(Chromosome chromosome, long start, long end) {
		String sql = String.format(QUERY_VARIANT_SUMMARY, chromosome.getChar(), start, end);
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					List<ClinvarVariantSummary> results = new ArrayList<>();
					while (resultSet.next()) {
						String reviewStatus = resultSet.getString("ReviewStatus");
						Integer numberSubmitters = resultSet.getInt("NumberSubmitters");
						String guidelines = resultSet.getString("Guidelines");

						results.add(new ClinvarVariantSummary(reviewStatus, numberSubmitters, guidelines));
					}

					if (results.isEmpty()) {
						return null;
					} else if (results.size() == 1) {
						return results.get(0);
					} else {
						//TODO Пока не найденно решение пытаемся найти "лучше", исходим: что лучше добавить неправильную, чем пропустить правильную.
						results.sort((o1, o2) -> {
							int i1 = (o1.reviewStatus.conflicts == null) ? 0 : (o1.reviewStatus.conflicts) ? 1 : 2;
							int i2 = (o2.reviewStatus.conflicts == null) ? 0 : (o2.reviewStatus.conflicts) ? 1 : 2;
							return i2 - i1;
						});
						ClinvarVariantSummary result = results.get(0);
						log.warn("WARNING!!! Many record({}), sql: {}, select: {}", results.size(), sql, result.reviewStatus.text);
						return result;
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex, "query: " + sql);
		}
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
