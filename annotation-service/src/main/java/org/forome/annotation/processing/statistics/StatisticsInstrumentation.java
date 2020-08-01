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

package org.forome.annotation.processing.statistics;

import graphql.annotations.dataFetchers.MethodDataFetcher;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.forome.annotation.utils.Statistics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class StatisticsInstrumentation extends SimpleInstrumentation {

	public final ConcurrentMap<String, Statistics> statistics;

	public StatisticsInstrumentation() {
		this.statistics = new ConcurrentHashMap<>();
	}

	@Override
	public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
		MethodDataFetcher source = (MethodDataFetcher) dataFetcher;
		return new DataFetcher<Object>() {
			@Override
			public Object get(DataFetchingEnvironment environment) throws Exception {
				long t1 = System.currentTimeMillis();
				Object result = source.get(environment);

				String path = parameters.getExecutionStepInfo().getPath().toList().stream()
						.filter(o -> o instanceof String)
						.map(o -> (String)o)
						.collect(Collectors.joining("/", "/",""));
				statistics.computeIfAbsent(
						path, s -> new Statistics()
				).addTime(
						System.currentTimeMillis() - t1
				);

				return result;
			}
		};
	}
}
