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

import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.makedatabase.MakeDatabase;
import org.forome.annotation.makedatabase.makesourcedata.MakeSourceData;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structure DB
 * <p>
 * GERP
 * +--------+------------+------+-----+---------+-------+
 * | Field  | Type       | Null | Key | Default | Extra |
 * +--------+------------+------+-----+---------+-------+
 * | GerpN  | double     | YES  |     | NULL    |       |
 * | Chrom  | varchar(4) | NO   | PRI | NULL    |       |
 * | Pos    | int(11)    | NO   | PRI | NULL    |       | <- hg19 position
 * | GerpRS | double     | YES  |     | NULL    |       |
 * +--------+------------+------+-----+---------+-------+
 * <p>
 * CONSERVATION
 * +------------+------------+------+-----+---------+-------+
 * | Field      | Type       | Null | Key | Default | Extra |
 * +------------+------------+------+-----+---------+-------+
 * | verPhCons  | double     | YES  |     | NULL    |       |
 * | mamPhyloP  | double     | YES  |     | NULL    |       |
 * | GerpN      | double     | YES  |     | NULL    |       |
 * | priPhyloP  | double     | YES  |     | NULL    |       |
 * | verPhyloP  | double     | YES  |     | NULL    |       |
 * | GerpRSpval | double     | YES  |     | NULL    |       |
 * | GerpRS     | double     | YES  |     | NULL    |       |
 * | priPhCons  | double     | YES  |     | NULL    |       |
 * | Pos        | int(11)    | NO   | PRI | NULL    |       | <- hg38 position
 * | mamPhCons  | double     | YES  |     | NULL    |       |
 * | Chrom      | varchar(4) | NO   | PRI | NULL    |       |
 * | GerpS      | double     | YES  |     | NULL    |       |
 * | hg19       | int(11)    | YES  | MUL | NULL    |       |
 * +------------+------------+------+-----+---------+-------+
 * <p>
 * Поля GerpN и GerpRS забираем из таблицы GERP, все остальные значения забираем из CONSERVATION
 */
public class MakeConservation implements MakeSourceData {

	private final MakeDatabase makeDatabase;

	public MakeConservation(MakeDatabase makeDatabase) {
		this.makeDatabase = makeDatabase;
	}

	public MakeConservationBuild getBatchRecord(Interval interval) throws SQLException {
		LiftoverConnector liftoverConnector = makeDatabase.liftoverConnector;
		Assembly assembly = makeDatabase.assembly;
		Chromosome chromosome = interval.chromosome;

		MakeConservationBuild.Data[] values = new MakeConservationBuild.Data[interval.end - interval.start + 1];

		Interval interval37 = liftoverConnector.toHG37(assembly, interval);
		if (interval37 != null) {
			try (Connection connection = makeDatabase.conservationConnector.databaseConnector.createConnection()) {
				try (Statement statement = connection.createStatement()) {
					String sql = String.format("select * from conservation.GERP where Chrom = '%s' and Pos >= %s and Pos <= %s order by Pos asc",
							chromosome.getChar(), interval37.start, interval37.end
					);
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							int iPos37 = resultSet.getInt("Pos");
							Position position = liftoverConnector.convertFromHG37(assembly, new Position(chromosome, iPos37));

							MakeConservationBuild.Data data = getAndCreate(values, position.value - interval.start);
							data.gerpN = resultSet.getFloat("GerpN");
							data.gerpRS = resultSet.getFloat("GerpRS");
						}
					}
				}
			}
		}


		Map<Integer, Integer> position38ToPosition = new HashMap<>();
		for (int pos = interval.start; pos <= interval.end; pos++) {
			Position position38 = liftoverConnector.toHG38(assembly, new Position(interval.chromosome, pos));
			if (position38 == null) continue;
			position38ToPosition.put(position38.value, pos);
		}
		if (!position38ToPosition.isEmpty()) {
			try (Connection connection = makeDatabase.conservationConnector.databaseConnector.createConnection()) {
				try (Statement statement = connection.createStatement()) {
					String sql = String.format("select * from conservation.CONSERVATION where Chrom = '%s' and Pos IN (%s)",
							chromosome.getChar(),
							position38ToPosition.keySet().stream()
									.map(position -> String.valueOf(position))
									.collect(Collectors.joining(","))
					);
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							int iPos38 = resultSet.getInt("Pos");
							int position = position38ToPosition.get(iPos38);

							MakeConservationBuild.Data data = getAndCreate(values, position - interval.start);
							data.priPhCons = toFloat(resultSet.getObject("priPhCons"));
							data.mamPhCons = toFloat(resultSet.getObject("mamPhCons"));
							data.verPhCons = toFloat(resultSet.getObject("verPhCons"));
							data.priPhyloP = toFloat(resultSet.getObject("priPhyloP"));
							data.mamPhyloP = toFloat(resultSet.getObject("mamPhyloP"));
							data.verPhyloP = toFloat(resultSet.getObject("verPhyloP"));
							data.gerpRSpval = toFloat(resultSet.getObject("gerpRSpval"));
							data.gerpS = toFloat(resultSet.getObject("gerpS"));
						}
					}
				}
			}
		}

		return new MakeConservationBuild(
				interval, values
		);
	}

	private static MakeConservationBuild.Data getAndCreate(MakeConservationBuild.Data[] values, int index) {
		MakeConservationBuild.Data data = values[index];
		if (data == null) {
			data = new MakeConservationBuild.Data();
			values[index] = data;
		}
		return data;
	}

	public Float toFloat(Object value) {
		if (value == null) {
			return null;
		}
		return ((Number) value).floatValue();
	}

	@Override
	public int getMinPosition(Chromosome chromosome) throws SQLException {
		if (makeDatabase.assembly != Assembly.GRCh37) {
			throw new IllegalArgumentException();
		}

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
		if (makeDatabase.assembly != Assembly.GRCh37) {
			throw new IllegalArgumentException();
		}

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
