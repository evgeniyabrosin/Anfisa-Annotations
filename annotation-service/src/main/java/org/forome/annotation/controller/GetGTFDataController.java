package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.gtf.struct.GTFResult;
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
 * http://localhost:8095/GetGTFData?session=...&data=[{"chromosome": "22", "position": 36691607},{"chromosome": "2", "position": 73675228}]
 * https://anfisa.forome.dev/annotationservice/GetGTFData?session=a57ee6662557402e9539373b31093a29&data=[{"chromosome": "22", "position": 36691607},{"chromosome": "2", "position": 73675228}]
 */
@Controller
@RequestMapping(value = {"/GetGTFData", "/annotationservice/GetGTFData"})
public class GetGTFDataController {

	private final static Logger log = LoggerFactory.getLogger(GetGTFDataController.class);

	public static class RequestItem {

		public final String chromosome;
		public final long position;

		public RequestItem(String chromosome, long position) {
			this.chromosome = chromosome;
			this.position = position;
		}
	}

	@RequestMapping(value = {"", "/"})
	public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
		log.debug("GetGTFDataController execute, time: {}", System.currentTimeMillis());

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

		CompletableFuture<JSONArray> future = new CompletableFuture<>();
		ExecutorServiceUtils.poolExecutor.execute(() -> {
			try {
				long t1 = System.currentTimeMillis();

				ArrayList<RequestItem> requestItems = parseRequestData(sRequestData);

				List<CompletableFuture<GTFResult>> futureGTFResults = new ArrayList<>();
				GTFConnector gtfConnector = service.getGtfConnector();
				for (RequestItem requestItem : requestItems) {
					futureGTFResults.add(gtfConnector.request(
							requestItem.chromosome,
							requestItem.position
					));
				}

				CompletableFuture.allOf(futureGTFResults.toArray(new CompletableFuture[futureGTFResults.size()]))
						.thenApply(v -> {
							JSONArray results = new JSONArray();
							for (int i = 0; i < requestItems.size(); i++) {
								RequestItem requestItem = requestItems.get(i);
								GTFResult gtfResult = futureGTFResults.get(i).join();

								JSONObject result = new JSONObject();
								result.put("input", new JSONArray() {{
									add(requestItem.chromosome);
									add(requestItem.position);
								}});
								result.put("gene", toJson(gtfResult));
								results.add(result);
							}

							long t2 = System.currentTimeMillis();
							log.debug("GetGTFDataController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

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
					log.debug("GetGTFDataController build response, time: {}", System.currentTimeMillis());
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

			long position = RequestParser.toLong("position", oItem.getAsString("position"));

			requestItems.add(new RequestItem(
					chromosome,
					position
			));
		}

		return requestItems;
	}

	private static JSONObject toJson(GTFResult gtfResult) {
		JSONObject out = new JSONObject();
		out.put("symbol", gtfResult.symbol);
		return out;
	}

}
