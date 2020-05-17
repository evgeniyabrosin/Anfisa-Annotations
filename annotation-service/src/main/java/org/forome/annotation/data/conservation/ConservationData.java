/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.conservation;

import com.google.common.collect.Lists;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.database.Source;
import org.forome.annotation.service.database.struct.record.Record;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.utils.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConservationData {

	private final static Logger log = LoggerFactory.getLogger(ConservationData.class);

	private final DatabaseConnectService databaseConnectService;

	private final Statistics statistics;

	public class GerpData {

		public final Float gerpN;
		public final Float gerpRS;

		public GerpData(Float gerpN, Float gerpRS) {
			this.gerpN = gerpN;
			this.gerpRS = gerpRS;
		}
	}

	public ConservationData(
			DatabaseConnectService databaseConnectService
	) {
		this.databaseConnectService = databaseConnectService;
		this.statistics = new Statistics();
	}

	public List<SourceMetadata> getSourceMetadata() {
		return Lists.newArrayList(new SourceMetadata("GERP", "hg19.GERP_scores", null));
	}

	public Conservation getConservation(Assembly assembly, Interval position, String ref, String alt) {
		if (alt.length() == 1 && ref.length() == 1) {
			//Однобуквенный вариант
			return getConservation(assembly, position);
		} else if (alt.length() > 1 && ref.length() == 1) {
			//Инсерция
			return getConservation(assembly, position);
		} else {
			return null;
		}
	}

	private Conservation getConservation(Assembly assembly, Interval interval) {
		GerpData gerpData = getGerpDataFromRocksDB(assembly, interval);

		if (gerpData != null) {
			Float gerpRS = (gerpData != null) ? gerpData.gerpRS : null;
			Float gerpN = (gerpData != null) ? gerpData.gerpN : null;
			return new Conservation(
					gerpRS, gerpN
			);
		} else {
			return null;
		}
	}

	private GerpData getGerpDataFromRocksDB(Assembly assembly, Interval interval) {
		long t1 = System.nanoTime();
		try {
			Source source = databaseConnectService.getSource(assembly);

			int minPosition;
			int maxPosition;
			if (interval.start <= interval.end) {
				minPosition = interval.start;
				maxPosition = interval.end;
			} else {
				//Инсерция
				//minPosition = maxPosition = pHG19.start;
				minPosition = Math.min(interval.start, interval.end - 1);
				maxPosition = Math.max(interval.start, interval.end - 1);
			}

			Float maxGerpN = null;
			Float maxGerpRS = null;
			for (int pos = minPosition; pos <= maxPosition; pos++) {
				Position position = new Position(
						interval.chromosome,
						pos
				);
				Record record = source.getRecord(position);
				if (record == null) {
					continue;
				}

				Conservation conservation = record.getConservation();
				if ((conservation.gerpN != null) && (maxGerpN == null || maxGerpN < conservation.gerpN)) {
					maxGerpN = conservation.gerpN;
				}
				if ((conservation.gerpRS != null) && (maxGerpRS == null || maxGerpRS < conservation.gerpRS)) {
					maxGerpRS = conservation.gerpRS;
				}
			}

			if (maxGerpN != null || maxGerpRS != null) {
				return new GerpData(maxGerpN, maxGerpRS);
			} else {
				return null;
			}
		} finally {
			statistics.addTime(System.nanoTime() - t1);
		}
	}

	public Statistics.Stat getStatistics() {
		return statistics.getStat();
	}

}
