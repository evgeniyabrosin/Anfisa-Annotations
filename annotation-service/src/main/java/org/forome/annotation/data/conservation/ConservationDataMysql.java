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

	public Conservation getConservation(Interval position, String ref, String alt) {
		if (alt.length() == 1 && ref.length() == 1) {
			//Однобуквенный вариант
			return getConservation(position);
		} else if (alt.length() > 1 && ref.length() == 1) {
			//Инсерция
			return getConservation(position);
		} else {
			return null;
		}
	}

	private Conservation getConservation(Interval pHG19) {
		GerpData gerpData = getGerpDataFromMysql(pHG19);
		//GerpData gerpData = getGerpDataFromRocksDB(pHG19);

		if (gerpData != null) {
			Float gerpRS = (gerpData != null) ? gerpData.gerpRS : null;
			Float gerpN = (gerpData != null) ? gerpData.gerpN : null;
			return new Conservation(
					gerpRS,  gerpN
			);
		} else {
			return null;
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
