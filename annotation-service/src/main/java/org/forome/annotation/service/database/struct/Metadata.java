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

package org.forome.annotation.service.database.struct;

import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.utils.bits.ShortBits;
import org.forome.annotation.utils.bits.StringBits;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Metadata {

	public static String KEY_FORMAT_VERSION = "version_format";
	public static String KEY_ASSEMBLY = "assembly";

	private final RocksDB rocksDB;
	private final ColumnFamilyHandle columnFamilyInfo;

	public Metadata(RocksDB rocksDB, ColumnFamilyHandle columnFamilyInfo) {
		this.rocksDB = rocksDB;
		this.columnFamilyInfo = columnFamilyInfo;
	}

	public short getFormatVersion() {
		try {
			byte[] bytes = rocksDB.get(columnFamilyInfo, StringBits.toByteArray(KEY_FORMAT_VERSION));
			return ShortBits.fromByteArray(bytes);
		} catch (RocksDBException e) {
			throw ExceptionBuilder.buildExternalDatabaseException(e);
		}
	}

	public Assembly getAssembly() {
		try {
			byte[] bytes = rocksDB.get(columnFamilyInfo, StringBits.toByteArray(KEY_ASSEMBLY));
			return Assembly.valueOf(StringBits.fromByteArray(bytes));
		} catch (RocksDBException e) {
			throw ExceptionBuilder.buildExternalDatabaseException(e);
		}
	}

}
