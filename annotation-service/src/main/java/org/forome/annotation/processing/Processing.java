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
import org.forome.annotation.processing.smavariant.SplitMAVariant;
import org.forome.annotation.processing.statistics.StatisticsInstrumentation;
import org.forome.annotation.processing.struct.GContext;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Sequence;
import org.forome.annotation.struct.mavariant.MAVariant;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantStruct;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.utils.Statistics;
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

	public final Statistics graphqlStatistics = new Statistics();
	public final Statistics anfisaStatistics = new Statistics();
	public final StatisticsInstrumentation statisticsInstrumentation = new StatisticsInstrumentation();

	public Processing(AnfisaConnector anfisaConnector, TypeQuery typeQuery) {
		this.anfisaConnector = anfisaConnector;

		GraphQLSchema graphQLSchema = AnnotationsSchemaCreator.newAnnotationsSchema()
				.query(GRecord.class)
				.build();

		graphQL = GraphQL
				.newGraphQL(graphQLSchema)
				.instrumentation(statisticsInstrumentation)
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
			MAVariant maVariant
	) {
		List<ProcessingResult> results = new ArrayList<>();
		for (Variant variant : SplitMAVariant.build(maVariant).split()) {

			//Валидируем ref
			validate(mCase.assembly, variant);

			ProcessingResult processingResult = exec(mCase, variant);
			results.add(processingResult);
		}
		return results;
	}

	public ProcessingResult exec(
			MCase mCase,
			Variant variant
	) {
		try {

			if (mCase == null) throw new IllegalArgumentException();
			if (variant == null) throw new IllegalArgumentException();

			JSONObject result = new JSONObject();

			long t1 = System.currentTimeMillis();
			AnfisaResult anfisaResult = anfisaConnector.build(
					new AnfisaInput.Builder(mCase.assembly).withSamples(mCase).build(),
					variant
			);
			anfisaStatistics.addTime(System.currentTimeMillis() - t1);

			result.merge(anfisaResult.toJSON());

			t1 = System.currentTimeMillis();
			ExecutionResult graphQLExecutionResult = graphQL.execute(
					ExecutionInput.newExecutionInput()
							.query(graphQLQuery)
							.variables(Collections.emptyMap())
							.context(
									new GContext(
											mCase, variant,
											anfisaConnector, anfisaResult.context
									)
							)
							.build()
			);
			if (!graphQLExecutionResult.getErrors().isEmpty()) {
				log.error("exception: " + graphQLExecutionResult.getErrors());
				throw new RuntimeException();
			}
			graphqlStatistics.addTime(System.currentTimeMillis() - t1);

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
					variant,
					result
			);
		} catch (Throwable e) {
			throw new RuntimeException("Exception build variant: " + variant.toString(), e);
		}
	}

	private void validate(Assembly assembly, Variant variant) {
		try {
			VariantStruct variantStruct = variant.variantStruct;
			VariantType variantType = variant.getVariantType();

			Interval interval;
			if (variantType == VariantType.SNV) {
				interval = variantStruct.interval;
			} else if (variantType == VariantType.INS) {
				interval = Interval.of(
						variantStruct.interval.chromosome,
						variantStruct.interval.start - 1,
						variantStruct.interval.start - 1
				);
			} else if (variantType == VariantType.DEL) {
				interval = Interval.of(
						variantStruct.interval.chromosome,
						variantStruct.interval.start - 1,
						variantStruct.interval.end
				);
			} else if (variantType == VariantType.SUBSTITUTION) {
				interval = Interval.of(
						variantStruct.interval.chromosome,
						variantStruct.interval.start,
						variantStruct.interval.start + variantStruct.ref.length() - 1
				);
				log.error("TODO Необходимо разрезать вариант!!! variant: {}", variant);
			} else {
				throw new RuntimeException("Unknown type: " + variantType);
			}

			Sequence sequence = anfisaConnector.fastaSource.getSequence(assembly, interval);
			if (!sequence.value.equalsIgnoreCase(variantStruct.ref.getBaseString())) {
				throw new RuntimeException("Not equals ref: " + variantStruct.ref.getBaseString() + ", and fasta: " + sequence.value);
			}
		} catch (Throwable e) {
			throw new RuntimeException("Variant: " + variant.toString(), e);
		}
	}
}
