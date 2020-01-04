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

package org.forome.annotation.service.ensemblvep.external;

import net.minidev.json.JSONObject;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class EnsemblVepExternalService implements EnsemblVepService {

	private final EnsemblVepHttpClient ensemblVepHttpClient;

	public EnsemblVepExternalService(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) throws IOException {
		this.ensemblVepHttpClient = new EnsemblVepHttpClient(uncaughtExceptionHandler);
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(Variant variant, String alternative) {
		String region = String.format("%s:%s:%s", variant.chromosome.getChar(), variant.getStart(), variant.end);
		String endpoint = String.format("/vep/human/region/%s/%s?hgvs=true&canonical=true&merged=true&protein=true&variant_class=true", region, alternative);
		return ensemblVepHttpClient.request(endpoint).thenApply(jsonArray -> (JSONObject) jsonArray.get(0));
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(Variant variant, String reference, String alternative) {
		return getVepJson(variant, alternative);
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(Chromosome chromosome, int start, int end, String alternative) {
		String region = String.format("%s:%s:%s", chromosome.getChar(), start, end);
		String endpoint = String.format("/vep/human/region/%s/%s?hgvs=true&canonical=true&merged=true&protein=true&variant_class=true", region, alternative);
		return ensemblVepHttpClient.request(endpoint).thenApply(jsonArray -> (JSONObject) jsonArray.get(0));
	}

	@Override
	public CompletableFuture<JSONObject> getVepJson(String id) {
		String endpoint = String.format("/vep/human/id/%s?hgvs=true&canonical=true&merged=true&protein=true&variant_class=true", id);
		return ensemblVepHttpClient.request(endpoint).thenApply(jsonArray -> (JSONObject) jsonArray.get(0));
	}

	@Override
	public void close() {
		ensemblVepHttpClient.close();
	}
}
