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

package org.forome.annotation.data.ref;

import org.forome.annotation.config.connector.RefConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

public class RefConnector implements AutoCloseable {

	private final DatabaseConnector databaseConnector;
	private final RefDataConnector refDataConnector;

	public RefConnector(
			DatabaseConnectService databaseConnectService,
			RefConfigConnector refConfigConnector
	) throws Exception {
		this.databaseConnector = new DatabaseConnector(databaseConnectService, refConfigConnector);
		this.refDataConnector = new RefDataConnector(databaseConnector);
	}

	public String getRef(Variant variant) {
		return refDataConnector.getRef(variant.chromosome, variant.getStart(), variant.end);
	}

	public String getRef(Chromosome chromosome, int start, int end) {
		return refDataConnector.getRef(chromosome, start, end);
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
