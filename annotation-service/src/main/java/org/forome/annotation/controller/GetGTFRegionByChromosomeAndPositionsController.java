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
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.struct.GTFResultLookup;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.network.authcontext.BuilderAuthContext;
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
 * http://localhost:8095/GetGTFRegionByChromosomeAndPositions?session=...&data=[{"chromosome": "5", "position": [478242, 144310]}]
 * https://anfisa.forome.dev/annotationservice/GetGTFRegionByChromosomeAndPositions?session=...&data=[{"chromosome": "5", "position": [478242, 144310]}]
 */
@Controller
@RequestMapping(value = {"/GetGTFRegionByChromosomeAndPositions", "/annotationservice/GetGTFRegionByChromosomeAndPositions"})
public class GetGTFRegionByChromosomeAndPositionsController {

	private final static Logger log = LoggerFactory.getLogger(GetGTFRegionByChromosomeAndPositionsController.class);

	public static class RequestItem {

		public final String chromosome;
		public final long[] positions;

		public RequestItem(String chromosome, long[] positions) {
			this.chromosome = chromosome;
			this.positions = positions;
		}
	}

	@RequestMapping(value = {"", "/"})
	public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
		Service service = Service.getInstance();

		BuilderAuthContext builderAuthContext = new BuilderAuthContext(service);
		builderAuthContext.auth(request);

		log.debug("GetGTFRegionByChromosomeAndPositionsController execute, time: {}", System.currentTimeMillis());

		String sRequestData = request.getParameter("data");
		if (Strings.isNullOrEmpty(sRequestData)) {
			throw ExceptionBuilder.buildInvalidValueException("data");
		}

		GTFConnector gtfConnector = service.getGtfConnector();
		if (gtfConnector == null) {
			throw ExceptionBuilder.buildInvalidOperation("inited");
		}

		CompletableFuture<JSONArray> future = new CompletableFuture<>();
		ExecutorServiceUtils.poolExecutor.execute(() -> {
			try {
				long t1 = System.currentTimeMillis();

				ArrayList<RequestItem> requestItems = parseRequestData(sRequestData);

				List<CompletableFuture<List<GTFResultLookup>>> futureGTFRegions = new ArrayList<>();
				for (RequestItem requestItem : requestItems) {
					futureGTFRegions.add(gtfConnector.getRegionByChromosomeAndPositions(
							requestItem.chromosome,
							requestItem.positions
					));
				}

				CompletableFuture.allOf(futureGTFRegions.toArray(new CompletableFuture[futureGTFRegions.size()]))
						.thenApply(v -> {
							JSONArray results = new JSONArray();
							for (int i = 0; i < requestItems.size(); i++) {
								RequestItem requestItem = requestItems.get(i);
								List<GTFResultLookup> resultLookups = futureGTFRegions.get(i).join();

								JSONObject result = new JSONObject();

								result.put("input", new JSONObject(){{
									put("chromosome", requestItem.chromosome);
									put("position", requestItem.positions);
								}});

								result.put("result", new JSONArray(){{
									for (GTFResultLookup resultLookup: resultLookups) {
										add(new JSONObject(){{
											put("transcript", resultLookup.transcript);
											put("gene", resultLookup.gene);
											put("position", resultLookup.position);
											put("region", resultLookup.region);
											put("index", resultLookup.index);
										}});
									}
								}});

								results.add(result);
							}

							long t2 = System.currentTimeMillis();
							log.debug("GetGTFRegionByChromosomeAndPositionsController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

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
					log.debug("GetGTFRegionByChromosomeAndPositionsController build response, time: {}", System.currentTimeMillis());
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

			String chromosome = RequestParser.toChromosome(oItem.getAsString("chromosome"));
			Object oPosition = RequestParser.toObject("position", oItem.get("position"));
			long[] positions;
			try {
				positions = ((JSONArray)oPosition).stream().map(o -> ((Number)o).longValue()).mapToLong(v -> v).toArray();
			} catch (Throwable e) {
				throw ExceptionBuilder.buildInvalidValueException("position", oPosition);
			}
			requestItems.add(new RequestItem(
					chromosome,
					positions
			));
		}

		return requestItems;
	}


}
