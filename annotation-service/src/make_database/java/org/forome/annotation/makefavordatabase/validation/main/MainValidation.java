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

package org.forome.annotation.makefavordatabase.validation.main;

import org.forome.annotation.makefavordatabase.main.Main;
import org.forome.annotation.makefavordatabase.validation.main.argument.Arguments;
import org.forome.annotation.makefavordatabase.validation.main.argument.ParserArgument;
import org.forome.annotation.service.database.rocksdb.favor.FavorDatabase;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainValidation {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		Arguments arguments;
		try {
			ParserArgument argumentParser = new ParserArgument(args);
			arguments = argumentParser.arguments;

		} catch (Throwable e) {
			log.error("Exception arguments parser", e);
			System.exit(2);
			return;
		}

		try {
			ReadOnlyRocksDBConnector readOnlyRocksDBConnector = new ReadOnlyRocksDBConnector(arguments.database.toAbsolutePath());

			int count = 0;
			try (RocksIterator rocksIterator = readOnlyRocksDBConnector.rocksDB.newIterator(readOnlyRocksDBConnector.getColumnFamily(FavorDatabase.COLUMN_FAMILY_DATA))) {
				rocksIterator.seekToFirst();
				while (rocksIterator.isValid()) {
					int order = FavorDatabase.getOrder(rocksIterator.key());
					if (count != order) {
						throw new RuntimeException("Exception validation, count: " + count + ", order: " + order);
					}

					if (count % 100_000 == 0) {
						log.debug("Processing validation order: " + order);
					}

					count++;
					rocksIterator.next();
				}
			}

			log.debug("Validation success");

			System.exit(0);
		} catch (Throwable e) {
			log.error("Exception", e);
			System.exit(1);
		}
	}

}