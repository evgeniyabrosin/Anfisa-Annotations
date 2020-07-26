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

package org.forome.annotation.data.gtf;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gtf.mysql.struct.GTFRegion;
import org.forome.annotation.data.gtf.mysql.struct.GTFResult;
import org.forome.annotation.data.gtf.mysql.struct.GTFResultLookup;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Position;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GTFConnector extends AutoCloseable {

	List<GTFTranscriptRow> getTranscriptRows(Assembly assembly, String transcript);

	CompletableFuture<GTFRegion> getRegion(AnfisaExecuteContext context, Assembly assembly, Position position, String transcript);

	Object[] lookup(AnfisaExecuteContext context, Assembly assembly, Position pos, String transcript);

	CompletableFuture<List<GTFResultLookup>> getRegionByChromosomeAndPositions(AnfisaExecuteContext context, String chromosome, long[] positions);

	CompletableFuture<GTFResult> request(Assembly assembly, String chromosome, long position);

	void close();

}
