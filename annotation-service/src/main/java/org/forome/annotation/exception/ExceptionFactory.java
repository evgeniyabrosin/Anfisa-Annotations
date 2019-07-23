package org.forome.annotation.exception;

import java.util.Map;

public class ExceptionFactory {

	public AnnotatorException build(String code, String comment, Map<String, Object> parameters) {
		return build(code, comment, parameters, null);
	}

	public AnnotatorException build(String code, Map<String, Object> parameters) {
		return build(code, null, parameters, null);
	}

	public AnnotatorException build(String code, String comment) {
		return build(code, comment, null, null);
	}

	public AnnotatorException build(String code, Throwable e) {
		return build(code, null, null, e);
	}

	public AnnotatorException build(String code) {
		return build(code, null, null, null);
	}

	public AnnotatorException build(String code, String comment, Map<String, Object> parameters, Throwable cause) {
		return new AnnotatorException(code, comment, parameters, cause);
	}
}
