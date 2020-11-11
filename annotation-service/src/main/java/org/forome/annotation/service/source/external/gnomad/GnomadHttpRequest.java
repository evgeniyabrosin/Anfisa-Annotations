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

package org.forome.annotation.service.source.external.gnomad;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.service.source.external.httprequest.HttpRequest;
import org.forome.annotation.service.source.external.source.ExternalSource;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Position;

/**
 * curl "localhost:8290/get?array=hg19&loc=18:67760501"
 * {"chrom": "chr18", "array": "hg19", "pos": 67760501, "Gerp": {"GerpN": 2.45, "GerpRS": -1.87}, "gnomAD": [{"ALT": "C", "REF": "A", "SOURCE": "g", "AC": 2, "AN": 31248, "AF": 6.4e-05, "nhomalt": 0, "faf95": 1.06e-05, "faf99": 1.096e-05, "male": {"AC": 1, "AN": 17400, "AF": 5.747e-05}, "female": {"AC": 1, "AN": 13848, "AF": 7.221e-05}, "afr": {"AC": 2, "AN": 8692, "AF": 0.0002301}, "amr": {"AC": 0, "AN": 842, "AF": 0}, "asj": {"AC": 0, "AN": 290, "AF": 0}, "eas": {"AC": 0, "AN": 1560, "AF": 0}, "fin": {"AC": 0, "AN": 3408, "AF": 0}, "nfe": {"AC": 0, "AN": 15380, "AF": 0}, "oth": {"AC": 0, "AN": 1076, "AF": 0}, "raw": {"AC": 2, "AN": 31416, "AF": 6.366e-05}, "hem": null}]}
 */
public class GnomadHttpRequest {

	private final ExternalSource httpSource;
	private final HttpRequest httpRequest;

	private final Assembly assembly;
	private final LiftoverConnector liftoverConnector;

	public GnomadHttpRequest(ExternalSource httpSource) {
		this.httpSource = httpSource;
		this.httpRequest = httpSource.httpDataSource.httpRequest;
		this.assembly = httpSource.assembly;
		this.liftoverConnector = httpSource.httpDataSource.liftoverConnector;
	}

	public JSONArray get(Position position) {
		Position position19 = liftoverConnector.toHG37(assembly, position);
		if (position19 == null) {
			return null;
		}

		JSONObject response = httpRequest.request(
				String.format("http://%s:%s/get?array=hg19&loc=%s:%s", httpRequest.url.getHost(), httpRequest.url.getPort(), position19.chromosome.getChar(), position19.value)
		);
		JSONArray jRecords = (JSONArray) response.get("gnomAD");
		return jRecords;
	}
}
