package org.forome.annotation.exception;


import com.infomaximum.database.exception.DatabaseException;

import java.util.Collections;
import java.util.HashMap;

public class ExceptionBuilder {

	private static final ExceptionFactory EXCEPTION_FACTORY = new ExceptionFactory();

	public static ServiceException buildDatabaseException(DatabaseException cause) {
		return EXCEPTION_FACTORY.build("database_error", cause);
	}

	public static ServiceException buildExternalDatabaseException(Throwable cause) {
		return EXCEPTION_FACTORY.build("external_database_error", cause);
	}

	public static ServiceException buildExternalServiceException(Throwable cause) {
		return EXCEPTION_FACTORY.build("external_service_error", cause);
	}

	public static ServiceException buildOperationException(Throwable cause) {
		return EXCEPTION_FACTORY.build("operation_error", cause);
	}

	public static ServiceException buildInvalidCredentialsException() {
		return EXCEPTION_FACTORY.build("invalid_credentials");
	}

	public static ServiceException buildNotUniqueValueException(String fieldName, Object fieldValue) {
		return EXCEPTION_FACTORY.build("not_unique_value", new HashMap<String, Object>() {{
			put("field_name", fieldName);
			put("field_value", fieldValue);
		}});
	}

	public static ServiceException buildInvalidValueException(String fieldName) {
		return EXCEPTION_FACTORY.build("invalid_value", new HashMap<String, Object>() {{
			put("field_name", fieldName);
		}});
	}

	public static ServiceException buildInvalidValueException(String fieldName, Object fieldValue) {
		return buildInvalidValueException(fieldName, fieldValue, null);
	}

	public static ServiceException buildInvalidValueException(String fieldName, Object fieldValue, String comment) {
		return EXCEPTION_FACTORY.build("invalid_value", comment, new HashMap<String, Object>() {{
			put("field_name", fieldName);
			put("field_value", fieldValue);
		}});
	}

	public static ServiceException buildInvalidValueJsonException(String fieldName, Throwable cause) {
		return EXCEPTION_FACTORY.build("invalid_value_json", null, new HashMap<String, Object>() {{
			put("field_name", fieldName);
		}}, cause);
	}

	public static ServiceException buildInvalidOperation(String comment) {
		return EXCEPTION_FACTORY.build("invalid_operation", comment);
	}

	public static ServiceException buildServerTimeoutException() {
		return EXCEPTION_FACTORY.build("server_timeout");
	}

	public static ServiceException buildServerBusyException(String cause) {
		return EXCEPTION_FACTORY.build("server_busy", Collections.singletonMap("cause", cause));
	}

	public static ServiceException buildServerOverloadedException() {
		return EXCEPTION_FACTORY.build("server_overloaded");
	}

	public static ServiceException buildServerShutsDownException() {
		return EXCEPTION_FACTORY.build("server_shuts_down");
	}
}


