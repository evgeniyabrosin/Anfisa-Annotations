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

package org.forome.annotation.data.spliceai.http;

import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.SourceMetadata;

import java.util.Collections;
import java.util.List;

/**
 * curl "http://localhost:8290/get?array=hg38&loc=chr14:18967988"
 */
public class SpliceAIConnectorHttp implements SpliceAIConnector {

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return Collections.emptyList();
	}

	@Override
	public SpliceAIResult getAll(String chromosome, long position, String ref, Allele altAllele) {
		return new SpliceAIResult("None", null, Collections.emptyMap());
	}

	@Override
	public void close() {

	}
}
