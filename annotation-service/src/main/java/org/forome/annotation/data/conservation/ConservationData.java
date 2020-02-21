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

package org.forome.annotation.data.conservation;

import org.forome.annotation.config.connector.ConservationConfigConnector;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.utils.Statistics;

import java.util.List;

public class ConservationData {

	private final ConservationDataMysql conservationConnectorMysql;

	public ConservationData(
			DatabaseConnectService databaseConnectService,
			ConservationConfigConnector conservationConfigConnector
	) {
		this.conservationConnectorMysql = new ConservationDataMysql(
				databaseConnectService, conservationConfigConnector
		);
	}

	public List<SourceMetadata> getSourceMetadata() {
		return conservationConnectorMysql.getSourceMetadata();
	}

	public Conservation getConservation(Interval position, String ref, String alt) {
		return conservationConnectorMysql.getConservation(position, ref, alt);
	}

	public Statistics.Stat getStatistics() {
		return conservationConnectorMysql.getStatistics();
	}

	public void close() {
		conservationConnectorMysql.close();
	}
}
