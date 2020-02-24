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
import org.forome.annotation.service.database.RocksDBDatabase;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.struct.Position;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Accumulation implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(Accumulation.class);

	protected final OptimisticTransactionDB rocksDB;
	protected final ColumnFamilyHandle columnFamily;

	private WriteBatchRecord activeWriteBatchRecord;

	public Accumulation(OptimisticTransactionDB rocksDB, ColumnFamilyHandle columnFamily) {
		this.rocksDB = rocksDB;
		this.columnFamily = columnFamily;
	}

	protected WriteBatchRecord getBatchRecord(Position position) throws RocksDBException {
		if (activeWriteBatchRecord == null) {
			BatchRecord batchRecord = RocksDBDatabase.getBatchRecord(rocksDB, columnFamily, position);
			if (batchRecord == null) {
				activeWriteBatchRecord = new WriteBatchRecord(RocksDBDatabase.getIntervalBatchRecord(position));
			} else {
				activeWriteBatchRecord = new WriteBatchRecord(batchRecord);
			}
		} else if (!activeWriteBatchRecord.interval.contains(position)) {
			flush();
			BatchRecord batchRecord = RocksDBDatabase.getBatchRecord(rocksDB, columnFamily, position);
			if (batchRecord == null) {
				activeWriteBatchRecord = new WriteBatchRecord(RocksDBDatabase.getIntervalBatchRecord(position));
			} else {
				activeWriteBatchRecord = new WriteBatchRecord(batchRecord);
			}
		}
		return activeWriteBatchRecord;
	}

	private void flush() throws RocksDBException {
		if (activeWriteBatchRecord == null) return;

		byte[] key = new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(activeWriteBatchRecord.interval);
		log.debug("key: {}", key);
		if (activeWriteBatchRecord.isEmpty()) {
			rocksDB.delete(columnFamily, key);
			log.debug("delete key: {}", key);
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
