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

package org.forome.annotation.data.hgmd;

import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.hgmd.struct.HgmdPmidRow;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.SourceMetadata;

import java.util.List;

public interface HgmdConnector extends AutoCloseable {

	class Data {
		public final List<HgmdPmidRow> hgmdPmidRows;
		public final List<String> phenotypes;

		public Data(List<HgmdPmidRow> hgmdPmidRows, List<String> phenotypes) {
			this.hgmdPmidRows = hgmdPmidRows;
			this.phenotypes = phenotypes;
		}
	}

	List<SourceMetadata> getSourceMetadata();

	List<String> getAccNum(Assembly assembly, String chromosome, long start, long end);

	HgmdConnectorMysql.Data getDataForAccessionNumbers(List<String> accNums);

	List<Long[]> getHg38(List<String> accNums);

	void close();
}
