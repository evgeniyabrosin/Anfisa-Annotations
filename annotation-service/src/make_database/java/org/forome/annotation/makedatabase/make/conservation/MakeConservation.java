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

package org.forome.annotation.makedatabase.make.conservation;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.makedatabase.make.MakeDatabase;
import org.forome.annotation.makedatabase.make.conservation.accumulation.AccumulationConservation;
import org.forome.annotation.makedatabase.statistics.StatisticsCompression;
import org.forome.annotation.struct.Chromosome;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Structure DB
 * <p>
 * GERP
 * +--------+------------+------+-----+---------+-------+
 * | Field  | Type       | Null | Key | Default | Extra |
 * +--------+------------+------+-----+---------+-------+
 * | GerpN  | double     | YES  |     | NULL    |       |
 * | Chrom  | varchar(4) | NO   | PRI | NULL    |       |
 * | Pos    | int(11)    | NO   | PRI | NULL    |       | <- hg19 position
 * | GerpRS | double     | YES  |     | NULL    |       |
 * +--------+------------+------+-----+---------+-------+
 * <p>
 * Исходные данные:
 * nohup wget http://mendel.stanford.edu/SidowLab/downloads/gerp/hg19.GERP_scores.tar.gz . &
 * nohup wget http://mendel.stanford.edu/SidowLab/downloads/gerp/hg18.GERP_scores.tar.gz &
 */
public class MakeConservation {

	private final static Logger log = LoggerFactory.getLogger(MakeConservation.class);

	private final MakeDatabase makeDatabase;
	private final Path gerpHg19;

	public MakeConservation(MakeDatabase makeDatabase, Path gerpHg19) {
		this.makeDatabase = makeDatabase;
		this.gerpHg19 = gerpHg19;
	}

	public void build(OptimisticTransactionDB rocksDB, ColumnFamilyHandle columnFamilyRecord, StatisticsCompression statistics) throws IOException, RocksDBException {
		log.debug("Write conservation...");
		try (GZIPInputStream isGZ = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(gerpHg19)))) {
			try (TarArchiveInputStream isTarGZ = new TarArchiveInputStream(isGZ)) {
				TarArchiveEntry entry;
				while ((entry = (TarArchiveEntry) isTarGZ.getNextEntry()) != null) {
					Chromosome chromosome = getChromosome(entry.getName());
					if (chromosome == null) continue;

					BufferedReader isItem = new BufferedReader(new InputStreamReader(isTarGZ));
					try (AccumulationConservation accumulation = new AccumulationConservation(rocksDB, columnFamilyRecord, statistics)) {

						String line;
						int position = 0;
						while ((line = isItem.readLine()) != null) {
							position++;

							String[] sLine = line.split("\\t");
							Float gerpN = null;
							if (!"0".equals(sLine[0])) {
								gerpN = Float.parseFloat(sLine[0]);
							}
							Float gerpRS = null;
							if (!"0".equals(sLine[1])) {
								gerpRS = Float.parseFloat(sLine[1]);
							}

							accumulation.add(
									chromosome,
									position,
									new Conservation(gerpRS, gerpN)
							);

							if (position % 1_000_000 == 0) {
								log.debug("Write chromosome: {}, position: {}", chromosome.toString(), position);
							}
						}
					}
				}
			}
		}
		log.debug("Write conservation... complete");
	}


	private static Chromosome getChromosome(String entryName) {
		Chromosome chromosome = Chromosome.of(entryName.split("\\.")[0]);
		if (chromosome.isSupport()) {
			return chromosome;
		} else {
			return null;
		}
	}

}
