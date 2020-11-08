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

package org.forome.annotation.data.anfisa.struct;

import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.dbsnp.DbSNPConnector;
import org.forome.annotation.service.source.external.astorage.struct.AStorageSource;
import org.forome.annotation.service.source.struct.source.Source;
import org.forome.annotation.struct.variant.Variant;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;
import org.forome.core.struct.sequence.Sequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnfisaExecuteContext {

	private final String CACHE_VARIANT_IDS = "variant_ids";
	private final String CACHE_MASKED_REGION = "masked_region";
	private final String CACHE_CDS_TRANSCRIPTS = "cds+transcripts";

	public final AnfisaInput anfisaInput;

	public final Variant variant;
	public final JSONObject vepJson;

	public Double gnomadAfFam;

	public AStorageSource sourceAStorageHttp;

	private final Map<String, Object> cache;

	public AnfisaExecuteContext(
			AnfisaInput anfisaInput,
			Variant variant,
			JSONObject vepJson
	) {
		this.anfisaInput = anfisaInput;

		this.variant = variant;
		this.vepJson = vepJson;

		this.cache = new HashMap<>();
	}

	public List<String> getVariantIds() {
		return (List<String>) cache.computeIfAbsent(CACHE_VARIANT_IDS, s -> {
			return new DbSNPConnector().getIds(this, variant);
		});
	}

	public boolean getMaskedRegion(AnfisaConnector anfisaConnector) {
		return (boolean) cache.computeIfAbsent(CACHE_MASKED_REGION, s -> {
			Assembly assembly = anfisaInput.mCase.assembly;
			Source source = anfisaConnector.sourceService.dataSource.getSource(assembly);

			Interval interval = Interval.of(
					variant.chromosome,
					variant.getStart(),
					(variant.getStart() < variant.end) ? variant.end : variant.getStart()
			);
			Sequence sequence = source.getFastaSequence(interval);
			String vSequence = sequence.getValue();

			//Если есть маленькие буквы, то мы имеем дело с замаскированными регионами тандемных повторов
			boolean maskedRegion = !vSequence.equals(vSequence.toUpperCase());

			return maskedRegion;
		});
	}

	public Set<String> getCdsTranscripts(AnfisaConnector anfisaConnector) {
		return (Set<String>) cache.computeIfAbsent(CACHE_CDS_TRANSCRIPTS, s -> {
			Assembly assembly = anfisaInput.mCase.assembly;
			return anfisaConnector.gtfConnector.getCdsTranscript(assembly, variant);
		});
	}
}
