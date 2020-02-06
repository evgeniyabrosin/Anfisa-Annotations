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

package org.forome.annotation.data.conservation;

import org.forome.annotation.config.connector.ConservationConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.service.database.struct.record.RecordConservation;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.utils.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ConservationDataMysql {

	private final static Logger log = LoggerFactory.getLogger(ConservationDataMysql.class);

	private final DatabaseConnectService databaseConnectService;

	public final DatabaseConnector databaseConnector;
	private final Statistics statistics;

	public class GerpData {

		public final Float gerpN;
		public final Float gerpRS;

		public GerpData(Float gerpN, Float gerpRS) {
			this.gerpN = gerpN;
			this.gerpRS = gerpRS;
		}
	}

	public ConservationDataMysql(
			DatabaseConnectService databaseConnectService,
			ConservationConfigConnector conservationConfigConnector
	) {
		this.databaseConnectService = databaseConnectService;

		this.databaseConnector = new DatabaseConnector(databaseConnectService, conservationConfigConnector);
		this.statistics = new Statistics();
	}

	public List<SourceMetadata> getSourceMetadata() {
		return databaseConnector.getSourceMetadata();
	}

	public Conservation getConservation(Interval position, Interval hg38, String ref, String alt) {
		if (alt.length() == 1 && ref.length() == 1) {
			//Однобуквенный вариант
			if (hg38 != null && !hg38.isSingle()) {
				throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", position.chromosome.getChar(), position));
			}
			return getConservation(position, hg38);
		} else if (alt.length() > 1 && ref.length() == 1) {
			//Инсерция
			return getConservation(position, hg38);
		} else {
			return null;
		}
	}

	private Conservation getConservation(Interval position, Interval hg38) {
		String sqlFromConservation = null;
		if (position.isSingle()) {
			if (hg38 != null) {
				sqlFromConservation = String.format("select priPhCons, mamPhCons, verPhCons, priPhyloP, mamPhyloP, " +
								"verPhyloP, GerpRSpval, GerpS from conservation.CONSERVATION where Chrom='%s' and Pos = %s",
						position.chromosome.getChar(), hg38.start
				);
			}
		} else {
			int hg38Pos1 = Integer.MIN_VALUE;
			int hg38Pos2 = Integer.MIN_VALUE;
			if (position.start > position.end) {
				if (hg38 != null) {
					hg38Pos1 = hg38.end - 1;
					hg38Pos2 = hg38.start;
					if (hg38.start <= hg38.end) {
						throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s, hg38: %s", position.chromosome.getChar(), position, hg38));
					}
				}
			} else {
				throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", position.chromosome.getChar(), position));
			}

			if (hg38 != null) {
				if (hg38Pos1 == Integer.MIN_VALUE || hg38Pos2 == Integer.MIN_VALUE) {
					throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", position.chromosome.getChar(), position));
				}
				sqlFromConservation = String.format("select max(priPhCons) as priPhCons, max(mamPhCons) as mamPhCons, " +
								"max(verPhCons) as verPhCons, max(priPhyloP) as priPhyloP, max(mamPhyloP) as mamPhyloP, " +
								"max(verPhyloP) as verPhyloP, max(GerpRSpval) as GerpRSpval, max(GerpS) as GerpS " +
								"from conservation.CONSERVATION where Chrom='%s' and Pos between %s and %s",
						position.chromosome.getChar(), hg38Pos1, hg38Pos2
				);
			}
		}
		return getConservation(position, sqlFromConservation);
	}

	private Conservation getConservation(Interval pHG19, String sqlFromConservation) {
		Double priPhCons = null;
		Double mamPhCons = null;
		Double verPhCons = null;
		Double priPhyloP = null;
		Double mamPhyloP = null;
		Double verPhyloP = null;
		Double gerpRSpval = null;
		Double gerpS = null;

		GerpData gerpData = getGerpDataFromMysql(pHG19);
		//GerpData gerpData = getGerpDataFromRocksDB(pHG19);

		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {

				boolean success = false;
				if (sqlFromConservation != null) {
					try (ResultSet resultSet = statement.executeQuery(sqlFromConservation)) {
						if (resultSet.next()) {
							priPhCons = (Double) resultSet.getObject("priPhCons");
							mamPhCons = (Double) resultSet.getObject("mamPhCons");
							verPhCons = (Double) resultSet.getObject("verPhCons");
							priPhyloP = (Double) resultSet.getObject("priPhyloP");
							mamPhyloP = (Double) resultSet.getObject("mamPhyloP");
							verPhyloP = (Double) resultSet.getObject("verPhyloP");
							gerpRSpval = (Double) resultSet.getObject("GerpRSpval");
							gerpS = (Double) resultSet.getObject("GerpS");
							success = true;
						}
					}
				}

				if (gerpData != null || success) {
					Float gerpRS = (gerpData != null) ? gerpData.gerpRS : null;
					Float gerpN = (gerpData != null) ? gerpData.gerpN : null;
					return new Conservation(
							priPhCons, mamPhCons,
							verPhCons, priPhyloP,
							mamPhyloP, verPhyloP,
							gerpRS, gerpRSpval,
							gerpN, gerpS
					);
				} else {
					return null;
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}

	private GerpData getGerpDataFromMysql(Interval pHG19) {
		long t1 = System.nanoTime();

		String sqlFromGerp;
		if (pHG19.isSingle()) {
			sqlFromGerp = String.format("select GerpN, GerpRS from conservation.GERP where Chrom='%s' and Pos = %s",
					pHG19.chromosome.getChar(), pHG19.start
			);
		} else {
			long hg19Pos1;
			long hg19Pos2;
			if (pHG19.start > pHG19.end) {
				hg19Pos1 = pHG19.end - 1;
				hg19Pos2 = pHG19.start;
			} else {
				throw new RuntimeException(String.format("Unknown state, chr: %s, position: %s", pHG19.chromosome.getChar(), pHG19));
			}
			sqlFromGerp = String.format("select max(GerpN) as GerpN, max(GerpRS) as GerpRS from conservation.GERP " +
					"where Chrom='%s' and Pos between %s and %s", pHG19.chromosome.getChar(), hg19Pos1, hg19Pos2);
		}

		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sqlFromGerp)) {
					if (resultSet.next()) {
						Double dGerpN = (Double) resultSet.getObject("GerpN");
						Float gerpN = (dGerpN == null) ? null : dGerpN.floatValue();

						Double dGerpRS = (Double) resultSet.getObject("GerpRS");
						Float gerpRS = (dGerpRS == null) ? null : dGerpRS.floatValue();

						return new GerpData(gerpN, gerpRS);
					} else {
						return null;
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		} finally {
			statistics.addTime(System.nanoTime() - t1);
		}
	}

	private GerpData getGerpDataFromRocksDB(Assembly assembly, Interval pHG19) {
		long t1 = System.nanoTime();

		try {

			int minPosition;
			int maxPosition;
			if (pHG19.start <= pHG19.end) {
				minPosition = pHG19.start;
				maxPosition = pHG19.end;
			} else {
				//Инсерция
				minPosition = pHG19.end;
				maxPosition = pHG19.start;
			}

			Float maxGerpN = null;
			Float maxGerpRS = null;
			for (int pos = minPosition; pos <= maxPosition; pos++) {
				Position position = new Position(
						pHG19.chromosome,
						pos
				);
				Record record = databaseConnectService.getSource(assembly).getRecord(position);
				RecordConservation recordConservation = record.getRecordConservation();

				if (maxGerpN == null || maxGerpN < recordConservation.getGerpN()) {
					maxGerpN = recordConservation.getGerpN();
				}
				if (maxGerpRS == null || maxGerpRS < recordConservation.getGerpRS()) {
					maxGerpRS = recordConservation.getGerpRS();
				}
			}

			if (maxGerpN != null || maxGerpRS != null) {
				return new GerpData(maxGerpN, maxGerpRS);
			} else {
				return null;
			}
		} finally {
			statistics.addTime(System.nanoTime() - t1);
		}
	}

	public Statistics.Stat getStatistics() {
		return statistics.getStat();
	}

	public void close() {
		databaseConnector.close();
	}

}
