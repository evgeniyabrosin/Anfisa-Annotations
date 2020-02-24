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

package org.forome.annotation.makedatabase.make.conservation.accumulation;

import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.makedatabase.make.accumulation.Accumulation;
import org.forome.annotation.makedatabase.make.batchrecord.WriteBatchRecord;
import org.forome.annotation.makedatabase.make.batchrecord.WriteBatchRecordConservation;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDBException;

public class AccumulationConservation extends Accumulation {

	public AccumulationConservation(OptimisticTransactionDB rocksDB, ColumnFamilyHandle columnFamilyRecord) {
		super(rocksDB, columnFamilyRecord);
	}

	public void add(Chromosome chromosome, int pos, Conservation conservation) throws RocksDBException {
		if (WriteBatchRecordConservation.isEmpty(conservation)) {
			return;
		}
		Position position = new Position(chromosome, pos);

		WriteBatchRecord writeBatchRecord = getBatchRecord(position);
		WriteBatchRecordConservation writeBatchRecordConservation = writeBatchRecord.getBatchRecordConservation();
		writeBatchRecordConservation.set(position, conservation);
	}


}
