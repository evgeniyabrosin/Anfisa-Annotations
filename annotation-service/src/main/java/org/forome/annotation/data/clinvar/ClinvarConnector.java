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

package org.forome.annotation.data.clinvar;

import org.forome.annotation.data.clinvar.struct.ClinvarResult;
import org.forome.annotation.data.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.Statistics;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;

import java.util.List;

public interface ClinvarConnector extends AutoCloseable {

	List<SourceMetadata> getSourceMetadata();

	List<ClinvarResult> getExpandedData(Assembly assembly, Variant variant);

	List<ClinvarResult> getData(Assembly assembly, String chromosome, long qStart, long qEnd, String alt);

	ClinvarVariantSummary getDataVariantSummary(Assembly assembly, Chromosome chromosome, long start, long end);

	Statistics getStatisticClinvarSubmitters();
	Statistics getStatisticClinvarData();
	Statistics getStatisticClinvarExpandedData();
	Statistics getStatisticClinvarVariantSummary();

	void close();
}
