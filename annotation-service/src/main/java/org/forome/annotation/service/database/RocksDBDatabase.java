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

package org.forome.annotation.service.database;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.RocksDBProvider;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.struct.Metadata;
import org.forome.annotation.service.database.struct.batch.BatchRecord;
import org.forome.annotation.service.database.struct.packer.PackInterval;
import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RocksDBDatabase implements Source {

	public static final short VERSION_FORMAT = 1;

	public static final String COLUMN_FAMILY_INFO = "info";
	public static final String COLUMN_FAMILY_RECORD = "record";

	public final Assembly assembly;
	public final Path pathDatabase;

	private final RocksDB rocksDB;
	private final Map<String, ColumnFamilyHandle> columnFamilies;

	private final Metadata metadata;

	public RocksDBDatabase(Assembly assembly, Path pathDatabase) throws DatabaseException {
		this.assembly = assembly;
		this.pathDatabase = pathDatabase;

		try (DBOptions options = buildOptions(pathDatabase)) {
			List<ColumnFamilyDescriptor> columnFamilyDescriptors = getColumnFamilyDescriptors(pathDatabase);

			List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
			rocksDB = RocksDB.openReadOnly(options, pathDatabase.toString(), columnFamilyDescriptors, columnFamilyHandles);

			columnFamilies = new HashMap<>();
			for (int i = 0; i < columnFamilyDescriptors.size(); i++) {
				String columnFamilyName = TypeConvert.unpackString(columnFamilyDescriptors.get(i).getName());
				ColumnFamilyHandle columnFamilyHandle = columnFamilyHandles.get(i);
				columnFamilies.put(columnFamilyName, columnFamilyHandle);
			}
		} catch (RocksDBException e) {
			throw new DatabaseException(e);
		}

		ColumnFamilyHandle columnFamilyInfo = getColumnFamily(COLUMN_FAMILY_INFO);
		if (columnFamilyInfo == null) {
			throw ExceptionBuilder.buildExternalDatabaseException("ColumnFamily not found");
		}
		this.metadata = new Metadata(rocksDB, columnFamilyInfo);
		if (metadata.getFormatVersion() != VERSION_FORMAT) {
			throw new RuntimeException("Format version RocksDB is not correct: " + metadata.getFormatVersion());
		}
		if (metadata.getAssembly() != assembly) {
			throw new RuntimeException("Not equals assembly: " + metadata.getAssembly());
		}
	}

	private ColumnFamilyHandle getColumnFamily(String name) {
		return columnFamilies.get(name);
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Record getRecord(Position position) {
		int k = position.value / BatchRecord.DEFAULT_SIZE;
		Interval interval = Interval.of(
				position.chromosome,
				k * BatchRecord.DEFAULT_SIZE,
				k * BatchRecord.DEFAULT_SIZE + BatchRecord.DEFAULT_SIZE - 1
		);
		try {
			byte[] bytes = rocksDB.get(
					getColumnFamily(COLUMN_FAMILY_RECORD),
					new PackInterval(BatchRecord.DEFAULT_SIZE).toByteArray(interval)
			);
			if (bytes == null) return null;

			BatchRecord batchRecord = new BatchRecord(interval, bytes);
			return batchRecord.getRecord(position);
		} catch (RocksDBException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}


	private static DBOptions buildOptions(Path pathDatabase) throws RocksDBException {
		final String optionsFilePath = pathDatabase.toString() + ".ini";

		DBOptions options = new DBOptions();
		if (Files.exists(Paths.get(optionsFilePath))) {
			final List<ColumnFamilyDescriptor> ignoreDescs = new ArrayList<>();
			OptionsUtil.loadOptionsFromFile(optionsFilePath, Env.getDefault(), options, ignoreDescs, false);
		} else {
			options
					.setInfoLogLevel(InfoLogLevel.WARN_LEVEL)
					.setMaxTotalWalSize(100L * SizeUnit.MB);
		}

		return options.setCreateIfMissing(true);
	}

	private static List<ColumnFamilyDescriptor> getColumnFamilyDescriptors(Path pathDatabase) throws RocksDBException {
		List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();

		try (Options options = new Options()) {
			for (byte[] columnFamilyName : RocksDB.listColumnFamilies(options, pathDatabase.toString())) {
				columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName));
			}
		}

		if (columnFamilyDescriptors.isEmpty()) {
			columnFamilyDescriptors.add(new ColumnFamilyDescriptor(TypeConvert.pack(RocksDBProvider.DEFAULT_COLUMN_FAMILY)));
		}

		return columnFamilyDescriptors;
	}
}
