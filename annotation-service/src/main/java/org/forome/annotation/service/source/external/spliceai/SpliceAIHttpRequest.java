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

package org.forome.annotation.service.source.external.spliceai;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.forome.annotation.service.source.external.httprequest.HttpRequest;
import org.forome.annotation.service.source.external.source.ExternalSource;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Interval;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * curl -d '{"variants":[{"chrom":"chr1","pos":16044572}], "fasta":"hg38", "arrays":["SpliceAI"]}' -H "Content-Type: application/json" -X POST "localhost:8290/collect"
 */
public class SpliceAIHttpRequest {

	private final ExternalSource httpSource;
	private final HttpRequest httpRequest;

	private final Assembly assembly;
	private final LiftoverConnector liftoverConnector;

	public SpliceAIHttpRequest(ExternalSource httpSource) {
		this.httpSource = httpSource;
		this.httpRequest = httpSource.httpDataSource.httpRequest;
		this.assembly = httpSource.assembly;
		this.liftoverConnector = httpSource.httpDataSource.liftoverConnector;
	}

	public JSONArray get(Interval interval) throws URISyntaxException {
		JSONObject params = new JSONObject();
		params.put("variants", new JSONArray() {{
			add(new JSONObject() {{
				put("chrom", interval.chromosome.getChromosome());
				put("pos", interval.start);
				put("last", (interval.start < interval.end) ? interval.end : interval.start);
			}});
		}});
		if (assembly == Assembly.GRCh37) {
			params.put("fasta", "hg19");
		} else if (assembly == Assembly.GRCh38) {
			params.put("fasta", "hg38");
		} else {
			throw new RuntimeException("Unknown assembly: " + assembly);
		}
		params.put("arrays", new JSONArray() {{
			add("SpliceAI");
		}});

		URI uri = new URI(String.format("http://%s:%s/collect", httpRequest.url.getHost(), httpRequest.url.getPort()));
		HttpPost httpPostRequest = new HttpPost(uri);
		httpPostRequest.setEntity(new StringEntity(params.toJSONString(), ContentType.APPLICATION_JSON));
		JSONObject response = httpRequest.request(httpPostRequest);

		JSONArray jRecords = (JSONArray) response.get("SpliceAI");
		return jRecords;
	}
}
