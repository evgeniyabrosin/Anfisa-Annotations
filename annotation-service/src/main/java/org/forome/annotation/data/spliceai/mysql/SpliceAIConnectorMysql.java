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

package org.forome.annotation.data.spliceai.mysql;

import org.forome.annotation.config.connector.SpliceAIConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.SourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpliceAIConnectorMysql implements SpliceAIConnector, AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(SpliceAIConnectorMysql.class);

	public final static float MAX_DS_UNLIKELY = 0.2f;

	private final DatabaseConnector databaseConnector;

	private final SpliceAIDataConnector spliceAIDataConnector;

	public SpliceAIConnectorMysql(
			DatabaseConnectService databaseConnectService,
			SpliceAIConfigConnector spliceAIConfigConnector
	) throws Exception {
		databaseConnector = new DatabaseConnector(databaseConnectService, spliceAIConfigConnector);
		spliceAIDataConnector = new SpliceAIDataConnector(databaseConnector);
	}

	@Override
	public List<SourceMetadata> getSourceMetadata(){
		return databaseConnector.getSourceMetadata();
	}

	@Override
	public SpliceAIResult getAll(String chromosome, long position, String ref, Allele altAllele){
		return spliceAIDataConnector.getAll(chromosome, position, ref, altAllele);
	}

	@Override
	public void close() {
		databaseConnector.close();
	}

}
