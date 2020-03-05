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

package org.forome.annotation.service.database.rocksdb;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.RocksDBProvider;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RocksDBDatabase {

	private final static Logger log = LoggerFactory.getLogger(RocksDBDatabase.class);

	public final Path pathDatabase;

	protected final RocksDB rocksDB;
	private final Map<String, ColumnFamilyHandle> columnFamilies;

	public RocksDBDatabase(Path pathDatabase) throws DatabaseException {
		this.pathDatabase = pathDatabase;

		log.debug("Load database: {}... ", pathDatabase.toString());
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
			log.error("Exception load database: {}", pathDatabase.toString(), e);
			throw new DatabaseException(e);
		}
		log.debug("Load database: {}... complete", pathDatabase.toString());
	}

	protected ColumnFamilyHandle getColumnFamily(String name) {
		return columnFamilies.get(name);
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
