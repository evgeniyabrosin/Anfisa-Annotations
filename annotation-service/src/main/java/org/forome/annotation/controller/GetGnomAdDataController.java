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

package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gnomad.struct.GnomadResult;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.network.authcontext.BuilderAuthContext;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.utils.ExecutorServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * http://localhost:8095/GetGnomAdData?session=...&data=[{"position": 36691607, "alternative": "C", "reference": "A", "chromosome": "22"}]
 * http://anfisa.forome.org/annotationservice/GetGnomAdData?session=a57ee6662557402e9539373b31093a29&data=[{"position": 36691607, "alternative": "C", "reference": "A", "chromosome": "22"}]
 */
@Controller
@RequestMapping(value = {"/GetGnomAdData", "/annotationservice/GetGnomAdData"})
public class GetGnomAdDataController {

	private final static Logger log = LoggerFactory.getLogger(GetGnomAdDataController.class);

	public static class RequestItem {

		public final Chromosome chromosome;
		public final int position;
		public final String reference;
		public final String alternative;

		public RequestItem(Chromosome chromosome, int position, String reference, String alternative) {
			this.chromosome = chromosome;
			this.position = position;
			this.reference = reference;
			this.alternative = alternative;
		}
	}

	@RequestMapping(value = {"", "/"})
	public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
		Service service = Service.getInstance();

		BuilderAuthContext builderAuthContext = new BuilderAuthContext(service);
		builderAuthContext.auth(request);

		log.debug("GetGnomAdDataController execute, time: {}", System.currentTimeMillis());

		String sRequestData = request.getParameter("data");
		if (Strings.isNullOrEmpty(sRequestData)) {
			throw ExceptionBuilder.buildInvalidValueException("data");
		}

		CompletableFuture<JSONArray> future = new CompletableFuture<>();
		ExecutorServiceUtils.poolExecutor.execute(() -> {
			try {
				long t1 = System.currentTimeMillis();

				ArrayList<RequestItem> requestItems = parseRequestData(sRequestData);

				List<CompletableFuture<GnomadResult>> futureGnomadResults = new ArrayList<>();
				GnomadConnectorImpl gnomadConnector = service.getGnomadConnector();
				for (RequestItem requestItem : requestItems) {
					futureGnomadResults.add(gnomadConnector.request(
							null,
							Assembly.GRCh37,
							requestItem.chromosome,
							requestItem.position,
							requestItem.reference,
							requestItem.alternative
					));
				}

				CompletableFuture.allOf(futureGnomadResults.toArray(new CompletableFuture[futureGnomadResults.size()]))
						.thenApply(v -> {
							JSONArray results = new JSONArray();
							for (int i = 0; i < requestItems.size(); i++) {
								RequestItem requestItem = requestItems.get(i);
								GnomadResult gnomadResult = futureGnomadResults.get(i).join();

								JSONObject result = new JSONObject();
								result.put("input", new JSONArray() {{
									add(requestItem.chromosome);
									add(requestItem.position);
									add(requestItem.reference);
									add(requestItem.alternative);
								}});
								result.put("gnomAD", toJson(gnomadResult));
								results.add(result);
							}

							long t2 = System.currentTimeMillis();
							log.debug("GetGnomAdDataController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

							future.complete(results);
							return null;
						})
						.exceptionally(ex -> {
							Throwable throwable = ex;
							if (ex instanceof CompletionException) {
								throwable = ex.getCause();
							}
							log.error("Exception execute request", throwable);
							future.completeExceptionally(throwable);
							return null;
						});
			} catch (Throwable ex) {
				log.error("Exception execute request", ex);
				future.completeExceptionally(ex);
			}
		});

		return future
				.thenApply(out -> {
					ResponseEntity responseEntity = ResponseBuilder.build(out);
					log.debug("GetGnomAdDataController build response, time: {}", System.currentTimeMillis());
					return responseEntity;

				})
				.exceptionally(throwable -> ResponseBuilder.build(throwable));
	}

	public static ArrayList<RequestItem> parseRequestData(String sRequestData) {
		ArrayList<RequestItem> requestItems = new ArrayList<>();
		JSONArray jRequestData;
		try {
			jRequestData = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(sRequestData);
		} catch (Throwable ex) {
			throw ExceptionBuilder.buildInvalidValueJsonException("data", ex);
		}
		for (Object item : jRequestData) {
			if (!(item instanceof JSONObject)) {
				throw ExceptionBuilder.buildInvalidValueException("data");
			}
			JSONObject oItem = (JSONObject) item;

			Chromosome chromosome = Chromosome.of(oItem.getAsString("chromosome"));

			int position = RequestParser.toInteger("position", oItem.getAsString("position"));

			String reference = oItem.getAsString("reference");
			if (Strings.isNullOrEmpty(reference)) {
				throw ExceptionBuilder.buildInvalidValueException("reference", reference);
			}

			String alternative = oItem.getAsString("alternative");
			if (Strings.isNullOrEmpty(alternative)) {
				throw ExceptionBuilder.buildInvalidValueException("alternative", alternative);
			}

			requestItems.add(new RequestItem(
					chromosome,
					position,
					reference,
					alternative
			));
		}

		return requestItems;
	}

	private static JSONObject toJson(GnomadResult gnomadResult) {
		if (gnomadResult == null) {
			return new JSONObject();
		}

		JSONObject out = new JSONObject();

		GnomadResult.Sum exomes = gnomadResult.exomes;
		if (exomes != null) {
			out.put("exomes", toJson(exomes));
		}

		GnomadResult.Sum genomes = gnomadResult.genomes;
		if (genomes != null) {
			out.put("genomes", toJson(genomes));
		}

		out.put("overall", toJson(gnomadResult.overall));

		if (gnomadResult.popmax != null) {
			out.put("popmax", gnomadResult.popmax.group.name());
			out.put("popmax_af", gnomadResult.popmax.af);
			out.put("popmax_an", gnomadResult.popmax.an);
		}
		if (gnomadResult.rawPopmax != null) {
			out.put("raw_popmax", gnomadResult.rawPopmax.group.name());
			out.put("raw_popmax_af", gnomadResult.rawPopmax.af);
			out.put("raw_popmax_an", gnomadResult.rawPopmax.an);
		}

		JSONArray jurls = new JSONArray();
		for (GnomadResult.Url url : gnomadResult.urls) {
			jurls.add(url.toString());
		}
		out.put("url", jurls);

		return out;
	}

	private static JSONObject toJson(GnomadResult.Sum gnomadResultSum) {
		JSONObject out = new JSONObject();
		out.put("AC", gnomadResultSum.ac);
		out.put("AF", gnomadResultSum.af);
		out.put("AN", gnomadResultSum.an);
		return out;
	}
}
