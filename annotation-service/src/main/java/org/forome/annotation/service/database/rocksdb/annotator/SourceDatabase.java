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

package org.forome.annotation.service.database.rocksdb.annotator;

import com.infomaximum.database.exception.DatabaseException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.Source;
import org.forome.annotation.service.database.rocksdb.RocksDBDatabase;
import org.forome.annotation.service.database.struct.Metadata;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.nio.file.Path;

public class SourceDatabase extends RocksDBDatabase implements Source {

	public static final short VERSION_FORMAT = 1;

	public static final String COLUMN_FAMILY_INFO = "info";
	public static final String COLUMN_FAMILY_RECORD = "record";

	public final Assembly assembly;

	private final Metadata metadata;

	public SourceDatabase(Assembly assembly, Path pathDatabase) throws DatabaseException {
		super(pathDatabase);
		this.assembly = assembly;

		ColumnFamilyHandle columnFamilyInfo = getColumnFamily(COLUMN_FAMILY_INFO);
		if (columnFamilyInfo == null) {
			throw ExceptionBuilder.buildExternalDatabaseException("ColumnFamily not found");
		}
		this.metadata = new Metadata(rocksDB, columnFamilyInfo);
		if (metadata.getFormatVersion() != VERSION_FORMAT) {
			throw new RuntimeException("Format version RocksDB is not correct: " + metadata.getFormatVersion());
		}
		if (metadata.getAssembly() != assembly) {
//TODO Ulitin V. Необходимо вернуть валидацию
//			throw new RuntimeException("Not equals assembly: " + metadata.getAssembly());
		}
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Record getRecord(Position position) {
		BatchRecord batchRecord = getBatchRecord(
				rocksDB,
				getColumnFamily(COLUMN_FAMILY_RECORD),
				position
		);
		if (batchRecord != null) {
			return batchRecord.getRecord(position);
		} else {
			return null;
		}
	}

	public static Interval getIntervalBatchRecord(Position position) {
		int k = position.value / BatchRecord.DEFAULT_SIZE;
		return Interval.of(
				position.chromosome,
				k * BatchRecord.DEFAULT_SIZE,
				k * BatchRecord.DEFAULT_SIZE + BatchRecord.DEFAULT_SIZE - 1
		);
	}

	public static BatchRecord getBatchRecord(RocksDB rocksDB, ColumnFamilyHandle columnFamilyHandle, Position position) {
		Interval interval = getIntervalBatchRecord(position);
		try {
			byte[] bytes = rocksDB.get(
					columnFamilyHandle,
					new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(interval)
			);
			if (bytes == null) return null;

			return new BatchRecord(interval, bytes);
		} catch (RocksDBException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}
}

