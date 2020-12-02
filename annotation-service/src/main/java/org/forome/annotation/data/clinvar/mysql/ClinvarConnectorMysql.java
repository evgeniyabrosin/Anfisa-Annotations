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

package org.forome.annotation.data.clinvar.mysql;

import org.forome.annotation.config.connector.ForomeConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.struct.ClinvarResult;
import org.forome.annotation.data.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.data.clinvar.struct.Row;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.utils.Statistics;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClinvarConnectorMysql implements ClinvarConnector, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(ClinvarConnectorMysql.class);

	private static final String SUBMITTER_QUERY = "SELECT SubmitterName, ClinicalSignificance FROM `forome`.`ClinVar_Submitters` NATURAL JOIN `forome`.`ClinVar2Sub_Sig` WHERE RCVaccession IN (%s)";

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
			"FROM `forome`.ClinVar_variant_summary AS v " +
			"WHERE " +
			"Assembly = 'GRCh37' AND " +
			"Chromosome='%s' AND " +
			"Start = %s ";

	private static final String QUERY_0 = QUERY_BASE + " AND Stop = %s ";
	private static final String QUERY_EXACT = QUERY_0 + " AND AlternateAllele = '%s'";
	private static final String QUERY_NA = QUERY_0 + " AND AlternateAllele = 'na'";

	private static final String QUERY_VARIANT_SUMMARY =
			"select ReviewStatus, NumberSubmitters, Guidelines from `forome`.ClinVar_variant_summary where Chromosome='%s' AND Start = %s and Stop = %s";

	private static final String CLINVAR_TYPE_SNV = "single nucleotide variant";
	private static final String CLINVAR_TYPE_DELETION = "deletion";

	private final LiftoverConnector liftoverConnector;
	private final DatabaseConnector databaseConnector;

	public final Statistics statisticClinvarSubmitters = new Statistics();
	public final Statistics statisticClinvarData = new Statistics();
	public final Statistics statisticClinvarExpandedData = new Statistics();
	public final Statistics statisticClinvarVariantSummary = new Statistics();

	public ClinvarConnectorMysql(DatabaseConnectService databaseConnectService, LiftoverConnector liftoverConnector, ForomeConfigConnector foromeConfigConnector) {
		this.liftoverConnector = liftoverConnector;
		this.databaseConnector = new DatabaseConnector(databaseConnectService, foromeConfigConnector);
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
//		return databaseConnector.getSourceMetadata();
		return Collections.emptyList();
	}

	private ClinvarResult getSubmitters(Row row) {
		long t1 = System.currentTimeMillis();
		try {
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
		} finally {
			statisticClinvarSubmitters.addTime(System.currentTimeMillis() - t1);
		}
	}

	private List<ClinvarResult> addSubmittersToRows(List<Row> rows) {
		List<ClinvarResult> results = new ArrayList<>();
		for (Row row : rows) {
			results.add(getSubmitters(row));
		}
		return results;
	}

	@Override
	public List<ClinvarResult> getExpandedData(Assembly assembly, Variant variant) {
		long t1 = System.currentTimeMillis();
		try {
			Position pStart = liftoverConnector.toHG37(assembly,
					new Position(variant.chromosome, variant.getStart())
			);
			if (pStart == null) {
				return Collections.emptyList();
			}

			List<Row> rows = new ArrayList<>();
			try (Connection connection = databaseConnector.createConnection()) {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery(String.format(
							QUERY_BASE, variant.chromosome.getChar(), pStart.value
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
		} finally {
			statisticClinvarExpandedData.addTime(System.currentTimeMillis() - t1);
		}
	}

	@Override
	public List<ClinvarResult> getData(Assembly assembly, String chromosome, long qStart, long qEnd, String alt) {
		long t1 = System.currentTimeMillis();
		try {

			Position pStart = liftoverConnector.toHG37(assembly,
					new Position(Chromosome.of(chromosome), (int) qStart)
			);
			Position pEnd = liftoverConnector.toHG37(assembly,
					new Position(Chromosome.of(chromosome), (int) qEnd)
			);
			if (pStart == null || pEnd == null) {
				return Collections.emptyList();
			}

			List<Row> rows = new ArrayList<>();
			try (Connection connection = databaseConnector.createConnection()) {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery(String.format(QUERY_EXACT, chromosome, pStart.value, pEnd.value, alt))) {
						while (resultSet.next()) {
							rows.add(_build(resultSet));
						}
					}

					if (rows.isEmpty()) {
						try (ResultSet resultSet = statement.executeQuery(String.format(QUERY_NA, chromosome, pStart.value, pEnd.value))) {
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
		} finally {
			statisticClinvarData.addTime(System.currentTimeMillis() - t1);
		}
	}

	private static Row _build(ResultSet resultSet) throws SQLException {
		int start = (int) resultSet.getLong("Start");
		int end = (int) resultSet.getLong("Stop");
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

	@Override
	public ClinvarVariantSummary getDataVariantSummary(Assembly assembly, Chromosome chromosome, long start, long end) {
		long t1 = System.currentTimeMillis();
		try {

			Position pStart = liftoverConnector.toHG37(assembly,
					new Position(chromosome, (int) start)
			);
			Position pEnd = liftoverConnector.toHG37(assembly,
					new Position(chromosome, (int) end)
			);
			if (pStart == null || pEnd == null) {
				return null;
			}

			String sql = String.format(QUERY_VARIANT_SUMMARY, chromosome.getChar(), pStart.value, pEnd.value);
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

		} finally {
			statisticClinvarVariantSummary.addTime(System.currentTimeMillis() - t1);
		}
	}

	@Override
	public Statistics getStatisticClinvarSubmitters() {
		return statisticClinvarSubmitters;
	}

	@Override
	public Statistics getStatisticClinvarData() {
		return statisticClinvarData;
	}

	@Override
	public Statistics getStatisticClinvarExpandedData() {
		return statisticClinvarExpandedData;
	}

	@Override
	public Statistics getStatisticClinvarVariantSummary() {
		return statisticClinvarVariantSummary;
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
