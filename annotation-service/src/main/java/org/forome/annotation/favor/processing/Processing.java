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

package org.forome.annotation.favor.processing;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.annotations.AnnotationsSchemaCreator;
import graphql.schema.GraphQLSchema;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.processing.graphql.record.GRecord;
import org.forome.annotation.favor.processing.struct.GContext;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;

public class Processing {

	private final static Logger log = LoggerFactory.getLogger(Processing.class);

	private final HgmdConnector hgmdConnector;

	private final GraphQL graphQL;

	private final String graphQLQuery;

	public Processing(HgmdConnector hgmdConnector) {
		this.hgmdConnector = hgmdConnector;

		GraphQLSchema graphQLSchema = AnnotationsSchemaCreator.newAnnotationsSchema()
				.query(GRecord.class)
				.build();

		graphQL = GraphQL
				.newGraphQL(graphQLSchema)
				.build();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("graphql/annotator/favor.graphql")) {
			graphQLQuery = IOUtils.toString(inputStream, "utf8");
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public ProcessingResult exec(Row row) {

		ExecutionResult graphQLExecutionResult = graphQL.execute(
				ExecutionInput.newExecutionInput()
						.query(graphQLQuery)
						.variables(Collections.emptyMap())
						.context(new GContext(
								hgmdConnector,
								row
						))
						.build()
		);
		if (!graphQLExecutionResult.getErrors().isEmpty()) {
			log.error("exception: " + graphQLExecutionResult.getErrors());
			throw new RuntimeException();
		}

		return new ProcessingResult(
				null,
				new JSONObject(graphQLExecutionResult.getData())
		);
	}

}
