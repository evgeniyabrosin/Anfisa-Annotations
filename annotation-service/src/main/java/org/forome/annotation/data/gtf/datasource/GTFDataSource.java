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

package org.forome.annotation.data.gtf.datasource;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.struct.variant.Variant;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Position;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

public interface GTFDataSource extends Closeable {

	List<GTFTranscriptRow> lookup(AnfisaExecuteContext context, Assembly assembly, Position position, String transcript);

	Set<String> getCdsTranscript(Assembly assembly, Variant variant);

	void close();

}
