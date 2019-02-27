package org.forome.annotation.exception;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

public class ServiceException extends RuntimeException {

	private final String code;
	private final Map<String, Object> parameters;
	private final String comment;

	ServiceException(String code, String comment, Map<String, Object> parameters, Throwable cause) {
		super(
				buildMessage(code, parameters, comment),
				cause
		);

		if (StringUtils.isEmpty(code)) {
			throw new IllegalArgumentException();
		}

		this.code = code;
		this.parameters = parameters == null ? null : Collections.unmodifiableMap(parameters);
		this.comment = comment;
	}

	public String getCode() {
		return code;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public String getComment() {
		return comment;
	}

	private static String buildMessage(String code, Map<String, Object> parameters, String comment) {
		StringJoiner builder = new StringJoiner(", ");
		if (comment != null) builder.add(comment);
		builder.add("code=" + code);
		if (parameters != null) builder.add("parameters=" + parameters);
		return builder.toString();
	}
}
