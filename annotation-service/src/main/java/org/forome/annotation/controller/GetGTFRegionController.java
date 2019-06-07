package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.gtf.struct.GTFRegion;
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
 * http://localhost:8095/GetGTFRegion?session=...&data=[{"transcript": "ENST00000456328", "position": 11870},{"transcript": "ENST00000518655", "position": 12226}]
 * https://anfisa.forome.dev/annotationservice/GetGTFData?session=...&data=[{"transcript": "ENST00000456328", "position": 11870},{"transcript": "ENST00000518655", "position": 12226}]
 */
@Controller
@RequestMapping(value = {"/GetGTFRegion", "/annotationservice/GetGTFRegion"})
public class GetGTFRegionController {

	private final static Logger log = LoggerFactory.getLogger(GetGTFRegionController.class);

	public static class RequestItem {

		public final String transcript;
		public final long position;

		public RequestItem(String transcript, long position) {
			this.transcript = transcript;
			this.position = position;
		}
	}

	@RequestMapping(value = {"", "/"})
	public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
		log.debug("GetGTFRegionController execute, time: {}", System.currentTimeMillis());

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

				List<CompletableFuture<GTFRegion>> futureGTFRegions = new ArrayList<>();
				for (RequestItem requestItem : requestItems) {
					futureGTFRegions.add(gtfConnector.getRegion(
							requestItem.transcript,
							requestItem.position
					));
				}

				CompletableFuture.allOf(futureGTFRegions.toArray(new CompletableFuture[futureGTFRegions.size()]))
						.thenApply(v -> {
							JSONArray results = new JSONArray();
							for (int i = 0; i < requestItems.size(); i++) {
								RequestItem requestItem = requestItems.get(i);
								GTFRegion gtfRegion = futureGTFRegions.get(i).join();

								JSONObject result = new JSONObject();

								result.put("input", new JSONObject(){{
									put("transcript", requestItem.transcript);
									put("position", requestItem.position);
								}});

								if (gtfRegion==null) {
									result.put("result", null);
								} else {
									result.put("result", new JSONObject(){{
										put("type", gtfRegion.typeRegion);
										put("index", gtfRegion.indexRegion);
									}});
								}

								results.add(result);
							}

							long t2 = System.currentTimeMillis();
							log.debug("GetGTFRegionController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

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

			String transcript = RequestParser.toString("transcript", oItem.getAsString("transcript"));

			long position = RequestParser.toLong("position", oItem.getAsString("position"));

			requestItems.add(new RequestItem(
					transcript,
					position
			));
		}

		return requestItems;
	}


}
