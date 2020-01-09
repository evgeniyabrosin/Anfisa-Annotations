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

package org.forome.annotation.makedatabase.makesourcedata.conservation;

import org.forome.annotation.makedatabase.MakeDatabase;
import org.forome.annotation.makedatabase.makesourcedata.MakeSourceData;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MakeConservation implements MakeSourceData {

	private final MakeDatabase makeDatabase;

	public MakeConservation(MakeDatabase makeDatabase) {
		this.makeDatabase = makeDatabase;
	}

	public MakeConservationBuild getBatchRecord(Interval interval) throws SQLException {
		MakeConservationBuild.GerpData[] values = new MakeConservationBuild.GerpData[interval.end - interval.start + 1];

		try (Connection connection = makeDatabase.conservationConnector.databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				String sql = String.format("select * from conservation.GERP where Chrom = '%s' and Pos >= %s and Pos <= %s order by Pos asc",
						interval.chromosome.getChar(), interval.start, interval.end
				);
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						int iPos = resultSet.getInt("Pos");
						float gerpN = resultSet.getFloat("GerpN");
						float gerpRS = resultSet.getFloat("GerpRS");
						values[iPos - interval.start] = new MakeConservationBuild.GerpData(
								gerpN, gerpRS
						);
					}
				}
			}
		}

		return new MakeConservationBuild(
				interval, values
		);
	}

	@Override
	public int getMinPosition(Chromosome chromosome) throws SQLException {
		int min;
		try (Connection connection = makeDatabase.conservationConnector.databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(
						String.format("select min(Pos) from conservation.GERP where Chrom = '%s'", chromosome.getChar())
				)) {
					if (!resultSet.next()) {
						throw new RuntimeException();
					}
					min = resultSet.getInt(1);
				}
				try (ResultSet resultSet = statement.executeQuery(
						String.format("select min(Pos) from conservation.CONSERVATION where Chrom = '%s'", chromosome.getChar())
				)) {
					if (!resultSet.next()) {
						throw new RuntimeException();
					}
					int hg38 = resultSet.getInt(1);
					Position hg19 = makeDatabase.liftoverConnector.toHG19(
							new Position(chromosome, hg38)
					);
					if (hg19 != null) {
						if (min > hg19.value) {
							min = hg19.value;
						}
					}
				}
			}
		}
		return min;
	}

	@Override
	public int getMaxPosition(Chromosome chromosome) throws SQLException {
		int max;
		try (Connection connection = makeDatabase.conservationConnector.databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(
						String.format("select max(Pos) from conservation.GERP where Chrom = '%s'", chromosome.getChar())
				)) {
					if (!resultSet.next()) {
						throw new RuntimeException();
					}
					max = resultSet.getInt(1);
				}
				try (ResultSet resultSet = statement.executeQuery(
						String.format("select max(Pos) from conservation.CONSERVATION where Chrom = '%s'", chromosome.getChar())
				)) {
					if (!resultSet.next()) {
						throw new RuntimeException();
					}
					int hg38 = resultSet.getInt(1);
					Position hg19 = makeDatabase.liftoverConnector.toHG19(
							new Position(chromosome, hg38)
					);
					if (hg19 != null) {
						if (max < hg19.value) {
							max = hg19.value;
						}
					}
				}
			}
		}
		return max;
	}

}
