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

package org.forome.annotation.data.fasta;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.utils.Statistics;
import org.forome.astorage.AStorage;
import org.forome.astorage.core.record.Record;
import org.forome.astorage.core.source.Source;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.nucleotide.Nucleotide;
import org.forome.core.struct.sequence.Sequence;

public class FastaSourceRocksDB implements FastaSource {

	private final AStorage aStorage;

	private final Statistics statistics;

	public FastaSourceRocksDB(AStorage aStorage) {
		this.aStorage = aStorage;

		this.statistics = new Statistics();
	}

	@Override
	public Sequence getSequence(AnfisaExecuteContext context, Assembly assembly, Interval interval) {
		return getSequence(assembly, interval);
	}

	@Override
	public Sequence getSequence(Assembly assembly, Interval interval) {
		Source source = aStorage.getSource(assembly);

		long t1 = System.currentTimeMillis();

		Nucleotide[] nucleotides = new Nucleotide[interval.end - interval.start + 1];
		for (int i = 0; i < nucleotides.length; i++) {
			Position position = new Position(interval.chromosome, interval.start + i);
			Record record = source.getRecord(position);

			Nucleotide nucleotide;
			if (record == null) {
				nucleotide = Nucleotide.NONE;
			} else {
				nucleotide = record.getNucleotide();
				if (nucleotide == null) {
					nucleotide = Nucleotide.NONE;
				}
			}
			nucleotides[i] = nucleotide;
		}

		Sequence sequence = new Sequence(interval, nucleotides);

		statistics.addTime(System.currentTimeMillis() - t1);

		return sequence;
	}

	@Override
	public Statistics.Stat getStatistics() {
		return statistics.getStat();
	}
}
