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

package org.forome.annotation.service.database.rocksdb.favor;

import com.infomaximum.database.exception.DatabaseException;
import org.forome.annotation.service.database.rocksdb.RocksDBDatabase;
import org.forome.annotation.utils.bits.IntegerBits;
import org.forome.annotation.utils.compression.GZIPCompression;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.nio.file.Path;

public class FavorDatabase extends RocksDBDatabase {

	public static final String COLUMN_FAMILY_DATA = "data";

	private static int NOT_COUNT_SIZE = Integer.MIN_VALUE;

	private volatile int _lazySize = NOT_COUNT_SIZE;

	public FavorDatabase(Path pathDatabase) throws DatabaseException {
		super(pathDatabase);
	}

	private ColumnFamilyHandle getDataColumnFamily() {
		return getColumnFamily(COLUMN_FAMILY_DATA);
	}

	public int getSize() {
		if (_lazySize == NOT_COUNT_SIZE) {
			synchronized (this) {
				if (_lazySize == NOT_COUNT_SIZE) {
					int count = 0;
					try (RocksIterator rocksIterator = rocksDB.newIterator(getDataColumnFamily())) {
						rocksIterator.seekToFirst();
						while (rocksIterator.isValid()) {
							count++;
							rocksIterator.next();
						}
					}
					_lazySize = count;
				}
			}
		}
		return _lazySize;
	}

	public String getRecord(int order) throws RocksDBException {
		byte[] key = FavorDatabase.getKey(order);
		byte[] value = rocksDB.get(getDataColumnFamily(), key);
		if (value == null) return null;

		return GZIPCompression.decompress(value);
	}

	public static byte[] getKey(int order) {
		return IntegerBits.toByteArray(order);
	}

}
