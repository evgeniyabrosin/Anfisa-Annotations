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

package org.forome.annotation.iterator.vepjson;

import net.minidev.json.JSONObject;
import org.forome.annotation.iterator.json.JsonFileIterator;
import org.forome.core.struct.Chromosome;

import java.io.InputStream;
import java.nio.file.Path;

public class VepJsonFileIterator extends JsonFileIterator {

	public VepJsonFileIterator(Path pathVepJson) {
		super(pathVepJson);
	}

	public VepJsonFileIterator(InputStream inputStream, boolean gzip) {
		super(inputStream, gzip);
	}

	@Override
	public JSONObject next() {
		JSONObject value;
		while (true) {
			value = super.next();
			if (!Chromosome.isSupportChromosome(value.getAsString("seq_region_name"))
			) {
				continue;//Игнорируем неподдерживаемые хромосомы
			}
			break;
		}
		return value;
	}

}
