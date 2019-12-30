/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.makedatabase.conservation;

import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.conservation.struct.BatchConservation;
import org.forome.annotation.connector.conservation.struct.ConservationItem;
import org.forome.annotation.makedatabase.MakeDatabase;
import org.forome.annotation.makedatabase.main.argument.ArgumentsMakeConservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.utils.packer.PackBatchConservation;
import org.forome.annotation.utils.packer.PackInterval;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class MakeConservation extends MakeDatabase {

	private final static Logger log = LoggerFactory.getLogger(MakeConservation.class);

	private final ConservationConnector conservationConnector;

	public MakeConservation(ArgumentsMakeConservation argumentsMakeConservation) throws Exception {
		super(argumentsMakeConservation);

		conservationConnector = new ConservationConnector(databaseConnectService, serviceConfig.conservationConfigConnector);
	}

	public void build() throws RocksDBException, SQLException {
		if (getColumnFamily(ConservationConnector.COLUMN_FAMILY_NAME) != null) {
			dropColumnFamily(ConservationConnector.COLUMN_FAMILY_NAME);
		}
		ColumnFamilyHandle columnFamilyHandle = createColumnFamily(ConservationConnector.COLUMN_FAMILY_NAME);

		for (Chromosome chromosome : Chromosome.values()) {

			Transaction transaction = rocksDB.beginTransaction(new WriteOptions());

			try (Connection connection = conservationConnector.databaseConnector.createConnection()) {
				try (Statement statement = connection.createStatement()) {

					//Ищем начальную позицию
					int ks;
					try (ResultSet resultSet = statement.executeQuery(
							String.format("select min(Pos) from conservation.GERP where Chrom = '%s'", chromosome.getChar())
					)) {
						if (!resultSet.next()) {
							throw new RuntimeException();
						}
						ks = resultSet.getInt(1) / PackInterval.DEFAULT_SIZE;
					}

					//Ищем конечную позицию
					int ke;
					try (ResultSet resultSet = statement.executeQuery(
							String.format("select max(Pos) from conservation.GERP where Chrom = '%s'", chromosome.getChar())
					)) {
						if (!resultSet.next()) {
							throw new RuntimeException();
						}
						ke = resultSet.getInt(1) / PackInterval.DEFAULT_SIZE;
					}

					for (int k = ks; k <= ke; k++) {
						int start = k * PackInterval.DEFAULT_SIZE;
						int end = start + PackInterval.DEFAULT_SIZE - 1;

						ConservationItem[] items = new ConservationItem[PackInterval.DEFAULT_SIZE];

						String sql = String.format("select * from conservation.GERP where Chrom = '%s' and Pos >= %s and Pos <= %s order by Pos asc",
								chromosome.getChar(), start, end
						);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							while (resultSet.next()) {
								int iPos = resultSet.getInt("Pos");
								float gerpN = resultSet.getFloat("GerpN");
								float gerpRS = resultSet.getFloat("GerpRS");
								items[iPos - start] = new ConservationItem(gerpN, gerpRS);
							}
						}

						//Если пакет не пустой, то записываем в базу
						if (!isEmptyConservationItem(items)) {

							Interval interval = new Interval(chromosome, start, end);
							BatchConservation batchConservation = new BatchConservation(interval, items);

							transaction.put(
									columnFamilyHandle,
									new PackInterval(PackInterval.DEFAULT_SIZE).toByteArray(interval),
									PackBatchConservation.toByteArray(batchConservation)
							);
						}

						if (start % 1000000 == 0) {
							log.debug("Write interval, chr: {}, pos: {}", chromosome, start);
						}
					}
				}
			}

			log.debug("Transaction commit...");

			transaction.commit();
			transaction.close();
		}

		rocksDB.compactRange(columnFamilyHandle);
	}

	private static boolean isEmptyConservationItem(ConservationItem[] items) {
		for (ConservationItem item : items) {
			if (item == null) continue;
			if (Math.abs(item.gerpN) > 0.00000001d) return false;
			if (Math.abs(item.gerpRS) > 0.00000001d) return false;
		}
		return true;
	}
}
