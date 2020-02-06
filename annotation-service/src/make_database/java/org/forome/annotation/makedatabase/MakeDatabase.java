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

package org.forome.annotation.makedatabase;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.conservation.ConservationDataMysql;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.makedatabase.main.argument.ArgumentsMake;
import org.forome.annotation.makedatabase.makesourcedata.conservation.MakeConservation;
import org.forome.annotation.makedatabase.makesourcedata.conservation.MakeConservationBuild;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.database.RocksDBDatabase;
import org.forome.annotation.service.database.struct.Metadata;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.utils.bits.ShortBits;
import org.forome.annotation.utils.bits.StringBits;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;


public class MakeDatabase implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(MakeDatabase.class);

	public final Assembly assembly;

	protected final DatabaseConnectService databaseConnectService;

	private final MakeDatabaseConnector makeDatabaseConnector;
	private final OptimisticTransactionDB rocksDB;

	public final LiftoverConnector liftoverConnector;
	public final ConservationDataMysql conservationConnector;

	public final MakeConservation makeConservation;

	public MakeDatabase(ArgumentsMake argumentsMake) throws Exception {
		this.assembly = argumentsMake.assembly;

		ServiceConfig serviceConfig = new ServiceConfig(argumentsMake.config);

		SSHConnectService sshTunnelService = new SSHConnectService();
		databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);

		this.makeDatabaseConnector = new MakeDatabaseConnector(databaseConnectService, argumentsMake.database.toAbsolutePath());
		this.rocksDB = makeDatabaseConnector.rocksDB;

		this.liftoverConnector = new LiftoverConnector();
		this.conservationConnector = new ConservationDataMysql(databaseConnectService, serviceConfig.conservationConfigConnector);

		this.makeConservation = new MakeConservation(this);
	}

	public void buildInfo() throws RocksDBException {
		if (makeDatabaseConnector.getColumnFamily(RocksDBDatabase.COLUMN_FAMILY_INFO) != null) {
			makeDatabaseConnector.dropColumnFamily(RocksDBDatabase.COLUMN_FAMILY_INFO);
		}
		ColumnFamilyHandle columnFamilyInfo = makeDatabaseConnector.createColumnFamily(RocksDBDatabase.COLUMN_FAMILY_INFO);

		//Версия формата
		rocksDB.put(
				columnFamilyInfo,
				StringBits.toByteArray(Metadata.KEY_FORMAT_VERSION),
				ShortBits.toByteArray(RocksDBDatabase.VERSION_FORMAT)
		);

		//Assembly
		rocksDB.put(
				columnFamilyInfo,
				StringBits.toByteArray(Metadata.KEY_ASSEMBLY),
				StringBits.toByteArray(assembly.name())
		);

		makeDatabaseConnector.rocksDB.compactRange(columnFamilyInfo);
	}

	public void buildRecords() throws RocksDBException, SQLException, IOException {
		if (makeDatabaseConnector.getColumnFamily(RocksDBDatabase.COLUMN_FAMILY_RECORD) != null) {
			makeDatabaseConnector.dropColumnFamily(RocksDBDatabase.COLUMN_FAMILY_RECORD);
		}
		ColumnFamilyHandle columnFamilyRecord = makeDatabaseConnector.createColumnFamily(RocksDBDatabase.COLUMN_FAMILY_RECORD);
		for (Chromosome chromosome : Chromosome.CHROMOSOMES) {
			int ks = getMinPosition(chromosome) / BatchRecord.DEFAULT_SIZE;
			int ke = getMaxPosition(chromosome) / BatchRecord.DEFAULT_SIZE;
			for (int k = ks; k <= ke; k++) {
				int start = k * BatchRecord.DEFAULT_SIZE;
				int end = start + BatchRecord.DEFAULT_SIZE - 1;
				Interval interval = Interval.of(chromosome, start, end);

				ByteArrayOutputStream os = new ByteArrayOutputStream();

				//Упаковываем conservation
				MakeConservationBuild conservation = makeConservation.getBatchRecord(interval);
				os.write(conservation.build());

				if (!conservation.isEmpty()) {
					rocksDB.put(
							columnFamilyRecord,
							new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(interval),
							os.toByteArray()
					);
				}

				if (start % 100000 == 0) {
					log.debug("Write, chromosome: {}, position: {}", chromosome, start);
				}
			}
		}
		makeDatabaseConnector.rocksDB.compactRange(columnFamilyRecord);
	}

	private int getMinPosition(Chromosome chromosome) throws SQLException {
		return makeConservation.getMinPosition(chromosome);
	}

	private int getMaxPosition(Chromosome chromosome) throws SQLException {
		return makeConservation.getMaxPosition(chromosome);
	}

	@Override
	public void close() {
		databaseConnectService.close();
	}

}
