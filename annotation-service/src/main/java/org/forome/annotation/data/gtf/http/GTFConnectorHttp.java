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

package org.forome.annotation.data.gtf.http;

import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.mysql.struct.GTFRegion;
import org.forome.annotation.data.gtf.mysql.struct.GTFResult;
import org.forome.annotation.data.gtf.mysql.struct.GTFResultLookup;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 *curl "localhost:8290/get?array=gtf&loc=12:885081-985081&feature=exon"
 */
public class GTFConnectorHttp implements GTFConnector {


	@Override
	public List<GTFTranscriptRow> getTranscriptRows(String transcript) {
		return Collections.emptyList();
	}

	@Override
	public CompletableFuture<GTFRegion> getRegion(String transcript, long position) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public Object[] lookup(long pos, String transcript) {
		return null;
	}

	@Override
	public CompletableFuture<List<GTFResultLookup>> getRegionByChromosomeAndPositions(String chromosome, long[] positions) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<GTFResult> request(String chromosome, long position) {
		return CompletableFuture.completedFuture(new GTFResult(null));
	}

	@Override
	public void close() {

	}
}
