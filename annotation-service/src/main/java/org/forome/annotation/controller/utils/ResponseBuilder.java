package org.forome.annotation.controller.utils;

import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.forome.annotation.Service;
import org.forome.annotation.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class ResponseBuilder {

	private static final String JSON_PROP_ERROR = "error";
	private static final String JSON_PROP_DATA = "data";

	public static ResponseEntity build(JSONAware data) {
		JSONObject out = new JSONObject();
		out.put(JSON_PROP_DATA, data);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
		headers.setExpires(0);

		return new ResponseEntity(out.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
	}

	public static ResponseEntity build(Throwable throwable) {
		Throwable exception;
		if (throwable instanceof CompletionException) {
			exception = throwable.getCause();
		} else {
			exception = throwable;
		}

		if (exception instanceof ServiceException) {
			return build((ServiceException) exception);
		} else {
			Service.getInstance().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
			return null;
		}
	}

	public static ResponseEntity build(ServiceException e) {
		JSONObject outError = new JSONObject();
		outError.put("code", e.getCode());
		if (e.getParameters() != null && !e.getParameters().isEmpty()) {
			JSONObject outParameters = new JSONObject();
			for (Map.Entry<String, Object> entry : e.getParameters().entrySet()) {
				outParameters.put(entry.getKey(), entry.getValue());
			}
			outError.put("parameters", outParameters);
		}
		if (e.getComment() != null) {
			outError.put("comment", e.getComment());
		}

		JSONObject out = new JSONObject();
		out.put(JSON_PROP_ERROR, outError);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
		headers.setExpires(0);

		return new ResponseEntity(out.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
