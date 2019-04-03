package org.forome.annotation.exception;

import java.util.Map;

public class ExceptionFactory {

	public ServiceException build(String code, String comment, Map<String, Object> parameters) {
		return build(code, comment, parameters, null);
	}

	public ServiceException build(String code, Map<String, Object> parameters) {
		return build(code, null, parameters, null);
	}

	public ServiceException build(String code, String comment) {
		return build(code, comment, null, null);
	}

	public ServiceException build(String code, Throwable e) {
		return build(code, null, null, e);
	}

	public ServiceException build(String code) {
		return build(code, null, null, null);
	}

	public ServiceException build(String code, String comment, Map<String, Object> parameters, Throwable cause) {
		return new ServiceException(code, comment, parameters, cause);
	}
}
