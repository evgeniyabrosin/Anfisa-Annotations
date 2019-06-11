package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.gtf.struct.GTFResultLookup;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
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
 * http://localhost:8095/GetGTFRegionByChromosomeAndPositions?session=...&data=[{"chromosome": "5", "position1": 478242, "position2": 144310}]
 * https://anfisa.forome.dev/annotationservice/GetGTFRegionByChromosomeAndPositions?session=...&data=[{"chromosome": "5", "position1": 478242, "position2": 144310}]
 */
@Controller
@RequestMapping(value = {"/GetGTFRegionByChromosomeAndPositions", "/annotationservice/GetGTFRegionByChromosomeAndPositions"})
public class GetGTFRegionByChromosomeAndPositionsController {

	private final static Logger log = LoggerFactory.getLogger(GetGTFRegionByChromosomeAndPositionsController.class);

	public static class RequestItem {

		public final String chromosome;
		public final long position1;
		public final long position2;

		public RequestItem(String chromosome, long position1, long position2) {
			this.chromosome = chromosome;
			this.position1 = position1;
			this.position2 = position2;
		}
	}

	@RequestMapping(value = {"", "/"})
	public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
		log.debug("GetGTFRegionByChromosomeAndPositionsController execute, time: {}", System.currentTimeMillis());

		Service service = Service.getInstance();

		String sessionId = request.getParameter("session");
		if (sessionId == null) {
			throw ExceptionBuilder.buildInvalidCredentialsException();
		}
		Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
		if (userId == null) {
			throw ExceptionBuilder.buildInvalidCredentialsException();
		}

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
							requestItem.position1,
							requestItem.position2
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
									put("position1", requestItem.position1);
									put("position2", requestItem.position2);
								}});

								result.put("result", new JSONArray(){{
									for (GTFResultLookup resultLookup: resultLookups) {
										add(new JSONObject(){{
											put("transcript", resultLookup.transcript);
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
			long position1 = RequestParser.toLong("position1", oItem.getAsString("position1"));
			long position2 = RequestParser.toLong("position2", oItem.getAsString("position2"));

			requestItems.add(new RequestItem(
					chromosome,
					position1,
					position2
			));
		}

		return requestItems;
	}


}
