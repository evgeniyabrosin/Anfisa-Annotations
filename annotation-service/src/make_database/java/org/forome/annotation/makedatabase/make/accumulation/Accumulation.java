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

package org.forome.annotation.makedatabase.make.accumulation;

import org.forome.annotation.makedatabase.make.batchrecord.WriteBatchRecord;
import org.forome.annotation.makedatabase.statistics.StatisticsCompression;
import org.forome.annotation.service.database.RocksDBDatabase;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.struct.Position;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDBException;

public class Accumulation implements AutoCloseable {

	protected final OptimisticTransactionDB rocksDB;
	protected final ColumnFamilyHandle columnFamily;
	protected final StatisticsCompression statistics;

	private WriteBatchRecord activeWriteBatchRecord;

	public Accumulation(OptimisticTransactionDB rocksDB, ColumnFamilyHandle columnFamily, StatisticsCompression statistics) {
		this.rocksDB = rocksDB;
		this.columnFamily = columnFamily;
		this.statistics = statistics;
	}

	protected WriteBatchRecord getBatchRecord(Position position) throws RocksDBException {
		if (activeWriteBatchRecord == null) {
			activeWriteBatchRecord = buildWriteBatchRecord(position);
		} else if (!activeWriteBatchRecord.interval.contains(position)) {
			flush();
			activeWriteBatchRecord = buildWriteBatchRecord(position);
		}
		return activeWriteBatchRecord;
	}

	private WriteBatchRecord buildWriteBatchRecord(Position position) {
		BatchRecord batchRecord = RocksDBDatabase.getBatchRecord(rocksDB, columnFamily, position);
		if (batchRecord == null) {
			return new WriteBatchRecord(RocksDBDatabase.getIntervalBatchRecord(position), statistics);
		} else {
			return new WriteBatchRecord(batchRecord, statistics);
		}
	}

	private void flush() throws RocksDBException {
		if (activeWriteBatchRecord == null) return;

		byte[] key = new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(activeWriteBatchRecord.interval);
		if (activeWriteBatchRecord.isEmpty()) {
			rocksDB.delete(columnFamily, key);
		} else {
			byte[] value = activeWriteBatchRecord.build();
			rocksDB.put(columnFamily, key, value);
		}
	}

	@Override
	public void close() throws RocksDBException {
		flush();
	}
}
