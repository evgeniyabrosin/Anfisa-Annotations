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

package org.forome.annotation.data.spliceai.datasource;

import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.spliceai.struct.Row;
import org.forome.annotation.service.source.struct.Source;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.core.struct.Assembly;

import java.io.Closeable;
import java.util.List;

public interface SpliceAIDataSource extends Closeable {

	List<Row> getAll(Source source, AnfisaExecuteContext context, Assembly assembly, String chromosome, int position, String ref, Allele altAllele);

	List<SourceMetadata> getSourceMetadata();

	void close();
}
