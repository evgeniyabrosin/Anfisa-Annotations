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

package org.forome.annotation.makefavordatabase.main;

import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.processing.Processing;
import org.forome.annotation.favor.utils.iterator.DumpIterator;
import org.forome.annotation.favor.utils.source.Source;
import org.forome.annotation.favor.utils.source.SourceLocal;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.favor.utils.struct.table.Table;
import org.forome.annotation.makedatabase.make.RocksDBConnector;
import org.forome.annotation.makefavordatabase.main.argument.Arguments;
import org.forome.annotation.makefavordatabase.main.argument.ParserArgument;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.database.rocksdb.favor.FavorDatabase;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.utils.compression.GZIPCompression;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Main {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		Arguments arguments;
		try {
			ParserArgument argumentParser = new ParserArgument(args);
			arguments = argumentParser.arguments;

			log.info("Arguments:");
			log.info("Source dump: {}", arguments.sourceDump);
			log.info("Target: {}", arguments.target);
			log.info("Offset: {}", arguments.offset);
			log.info("Limit: {}", arguments.limit);
		} catch (Throwable e) {
			log.error("Exception arguments parser", e);
			System.exit(2);
			return;
		}

		try {
			ServiceConfig serviceConfig = new ServiceConfig();
			SSHConnectService sshTunnelService = new SSHConnectService();
			DatabaseConnectService databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
			HgmdConnector hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);

//            Source source = new SourceRemote(Source.PATH_FILE);
			Source source = new SourceLocal(arguments.sourceDump);

			RocksDBConnector rocksDBConnector = new RocksDBConnector(arguments.target.toAbsolutePath());
			ColumnFamilyHandle columnFamily = getColumnFamily(rocksDBConnector);

			Processing processing = new Processing(hgmdConnector);
			try (InputStream is = source.getInputStream()) {
				try (BufferedReader bf = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))) {
					DumpIterator dumpIterator = new DumpIterator(bf);

					Table currentTable = null;

					while (dumpIterator.hasNext()) {
						Row row = dumpIterator.next();
						if (row.order< arguments.offset) {
							continue;
						}
						if (row.order >= arguments.limit) {
							break;
						}

						if (!row.table.equals(currentTable)) {
							currentTable = row.table;
							System.out.println("Table: " + currentTable.name);
							System.out.println(currentTable.fields.stream().map(field -> field.name).collect(Collectors.joining(", ")));
						}

						ProcessingResult processingResult = processing.exec(row);
						String record = processingResult.toJSON().toJSONString();

						rocksDBConnector.rocksDB.put(
								columnFamily,
								FavorDatabase.getKey(row.order),
								GZIPCompression.compress(record)
						);

						if (row.order % 100_000 == 0) {
							log.debug("Processing row order: " + row.order);
						}
					}
				}
			}

			log.debug("Compact...");
			rocksDBConnector.rocksDB.compactRange(columnFamily);
			log.debug("Compact...complete");

			source.close();

			System.exit(0);
		} catch (Throwable e) {
			log.error("Exception", e);
			System.exit(1);
		}
	}

	public static ColumnFamilyHandle getColumnFamily(RocksDBConnector rocksDBConnector) throws RocksDBException {
		ColumnFamilyHandle columnFamilyHandle = rocksDBConnector.getColumnFamily(FavorDatabase.COLUMN_FAMILY_DATA);
		if (columnFamilyHandle == null) {
			columnFamilyHandle = rocksDBConnector.createColumnFamily(FavorDatabase.COLUMN_FAMILY_DATA);
		}
		return columnFamilyHandle;
	}
}
