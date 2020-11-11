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

package org.forome.annotation.service.source.external.astorage;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.forome.annotation.service.source.external.astorage.struct.AStorageSource;
import org.forome.annotation.service.source.external.httprequest.HttpRequest;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.Statistics;
import org.forome.core.struct.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * curl -d '{"variants":[{"chrom":"chr3","pos":38603929}], "fasta":"hg38"}' -H "Content-Type: application/json" -X POST "localhost:8290/collect"
 * <p>
 * curl "localhost:8290/get?array=hg38&loc=3:38603929&alt=C"
 */
public class AStorageHttp {

	private final static Logger log = LoggerFactory.getLogger(AStorageHttp.class);

	private final HttpRequest httpRequest;

	private final Statistics statistics;

	public AStorageHttp(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;

		this.statistics = new Statistics();
	}

	public AStorageSource get(Assembly assembly, Variant variant) {
		JSONObject params = new JSONObject();
		params.put("variants", new JSONArray() {{
			add(new JSONObject() {{
				put("chrom", variant.chromosome.getChromosome());
				put("pos", variant.getStart());
				put("last", (variant.getStart() < variant.end) ? variant.end : variant.getStart());
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
			add("dbNSFP");
		}});

		int attempts = 5;
		while (true) {
			JSONObject response = null;
			try {
				long t1 = System.currentTimeMillis();
				response = request(params);
				statistics.addTime(System.currentTimeMillis() - t1);
				return new AStorageSource(assembly, response);
			} catch (Throwable t) {
				if (attempts-- > 0) {
					log.error("Exception request, last attempts: {}", attempts, t);
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {
					}
					continue;
				} else {
					throw t;
				}
			} finally {
				if (assembly == Assembly.GRCh37) {
					int resultPos = response.getAsNumber("pos").intValue();
					if (variant.getStart() != resultPos) {
						throw new RuntimeException("request: " + params.toJSONString() + "response: " + response);
					}
				}
			}
		}
	}

	private JSONObject request(JSONObject params) {
		try {
			URI uri = new URI(String.format("http://%s:%s/collect", httpRequest.url.getHost(), httpRequest.url.getPort()));
			HttpPost httpPostRequest = new HttpPost(uri);
			httpPostRequest.setEntity(new StringEntity(params.toJSONString(), ContentType.APPLICATION_JSON));
			return httpRequest.request(httpPostRequest);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Statistics.Stat getStatistics() {
		return statistics.getStat();
	}
}
