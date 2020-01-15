/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.processing;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.annotations.AnnotationsSchemaCreator;
import graphql.schema.GraphQLSchema;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.IOUtils;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaInput;
import org.forome.annotation.data.anfisa.struct.AnfisaResult;
import org.forome.annotation.processing.graphql.record.GRecord;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Processing {

	private final static Logger log = LoggerFactory.getLogger(Processing.class);

	private final AnfisaConnector anfisaConnector;
	private final GraphQL graphQL;

	private final String graphQLQuery;

	public Processing(AnfisaConnector anfisaConnector, TypeQuery typeQuery) {
		this.anfisaConnector = anfisaConnector;

		GraphQLSchema graphQLSchema = AnnotationsSchemaCreator.newAnnotationsSchema()
				.query(GRecord.class)
				.build();

		graphQL = GraphQL
				.newGraphQL(graphQLSchema)
				.build();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("graphql/annotator/" + typeQuery.fileNameGraphQLQuery)) {
			graphQLQuery = IOUtils.toString(inputStream, "utf8");
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public AnfisaConnector getAnfisaConnector() {
		return anfisaConnector;
	}

	public List<ProcessingResult> exec(
			MCase mCase,
			Variant variant
	) {
		List<ProcessingResult> results = new ArrayList<>();
		for (Allele altAllele : variant.getAltAllele()) {
			results.add(
					exec(mCase, variant, altAllele)
			);
		}
		return results;
	}

	private ProcessingResult exec(
			MCase mCase,
			Variant variant, Allele altAllele
	) {

		JSONObject result = new JSONObject();

		AnfisaResult anfisaResult = anfisaConnector.build(
				new AnfisaInput.Builder().withSamples(mCase).build(),
				variant, altAllele
		);
		result.merge(anfisaResult.toJSON());

		ExecutionResult graphQLExecutionResult = graphQL.execute(
				ExecutionInput.newExecutionInput()
						.query(graphQLQuery)
						.variables(Collections.emptyMap())
						.context(
								new GContext(mCase, variant, altAllele)
						)
						.build()
		);
		if (!graphQLExecutionResult.getErrors().isEmpty()) {
			log.error("exception: " + graphQLExecutionResult.getErrors());
			throw new RuntimeException();
		}

		//TODO Ulitin V. удалить временный костыль, введен из-за проблем мержинга данных
		JSONObject graphQLResult;
		try {
			graphQLResult = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(
					new JSONObject(graphQLExecutionResult.getData()).toJSONString(JSONStyle.NO_COMPRESS)
			);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		result.merge(graphQLResult);

		return new ProcessingResult(
				variant, altAllele,
				result
		);
	}
}
