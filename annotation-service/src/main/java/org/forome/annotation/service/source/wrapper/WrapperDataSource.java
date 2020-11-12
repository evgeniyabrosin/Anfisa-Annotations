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

package org.forome.annotation.service.source.wrapper;

import org.forome.annotation.annotator.AnnotationConsole;
import org.forome.annotation.service.source.DataSource;
import org.forome.annotation.service.source.struct.Source;
import org.forome.core.struct.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WrapperDataSource implements DataSource {

	private final static Logger log = LoggerFactory.getLogger(AnnotationConsole.class);

	private final HashMap<Assembly, WrapperSource> sources;

	public WrapperDataSource(DataSource dataSource) {
		this.sources = new HashMap<>();
		for (Assembly assembly : Assembly.values()) {
			sources.put(assembly, new WrapperSource(dataSource.getSource(assembly)));
		}
	}

	@Override
	public Source getSource(Assembly assembly) {
		return sources.get(assembly);
	}

	public void printStatistics() {
		for (Assembly assembly : Assembly.values()) {
			WrapperSource wrapperSource = sources.get(assembly);
			if (wrapperSource.isEmptyStatistics()) continue;

			log.debug("************{}**********", assembly);
			wrapperSource.printStatistics();
			log.debug("************END************");
		}
	}

}
