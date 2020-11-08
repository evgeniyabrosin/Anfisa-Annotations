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

package org.forome.annotation.service.source.external.conservation;

import net.minidev.json.JSONObject;
import org.forome.annotation.service.source.external.httprequest.HttpRequest;
import org.forome.annotation.service.source.external.source.ExternalSource;
import org.forome.astorage.core.data.Conservation;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Position;

/**
 * curl "localhost:8290/get?array=hg19&loc=12:885081"
 */
public class ConservationHttpRequest {

	private final ExternalSource httpSource;
	private final HttpRequest httpRequest;

	private final Assembly assembly;
	private final LiftoverConnector liftoverConnector;

	public ConservationHttpRequest(ExternalSource httpSource) {
		this.httpSource = httpSource;
		this.httpRequest = httpSource.httpDataSource.httpRequest;
		this.assembly = httpSource.assembly;
		this.liftoverConnector = httpSource.httpDataSource.liftoverConnector;
	}

	public Conservation getConservation(Position position) {
		Position position19 = liftoverConnector.toHG37(assembly, position);
		if (position19 == null) {
			return new Conservation(null, null);
		}

		String url = String.format("http://%s:%s/get?array=hg19&loc=%s:%s",
				httpRequest.url.getHost(), httpRequest.url.getPort(),
				position19.chromosome.getChar(), position19.value
		);

		JSONObject response = httpRequest.request(url);
		JSONObject jGerp = (JSONObject) response.get("Gerp");
		if (jGerp == null) {
			return new Conservation(null, null);
		} else {
			Float gerpRS = toFloat(jGerp.getAsNumber("GerpRS"));
			Float gerpN = toFloat(jGerp.getAsNumber("GerpN"));

			return new Conservation(gerpRS, gerpN);
		}
	}

	private static Float toFloat(Number number) {
		if (number == null) {
			return null;
		} else {
			float value = number.floatValue();
			if (Math.abs(value) < 0.00000001) {
				return null;
			} else {
				//Обрезаем число, для совместимости между сетевым и внутренним истоником
				return ((short) (value * 1000)) / 1000.0f;
			}
		}
	}
}
