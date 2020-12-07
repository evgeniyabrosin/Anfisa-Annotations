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

package org.forome.annotation.data.pharmgkb;

import org.forome.annotation.data.anfisa.struct.AnfisaResultView;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.utils.Statistics;

import java.util.List;

public interface PharmGKBConnector extends AutoCloseable {

	List<SourceMetadata> getSourceMetadata();

	List<AnfisaResultView.Pharmacogenomics.Item> getNotes(String variantId);

	List<AnfisaResultView.Pharmacogenomics.Item> getPmids(String variantId);

	List<AnfisaResultView.Pharmacogenomics.Item> getDiseases(String variantId);

	List<AnfisaResultView.Pharmacogenomics.Item> getChemicals(String variantId);

	Statistics getStatisticNotes();

	Statistics getStatisticPmids();

	Statistics getStatisticDiseases();

	Statistics getStatisticChemicals();

	void close();

}
