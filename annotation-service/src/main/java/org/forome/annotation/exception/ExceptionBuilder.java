package org.forome.annotation.exception;

import com.infomaximum.database.exception.DatabaseException;
import net.minidev.json.parser.ParseException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class ExceptionBuilder {

	private static final ExceptionFactory EXCEPTION_FACTORY = new ExceptionFactory();

	public static AnnotatorException buildDatabaseException(DatabaseException cause) {
		return EXCEPTION_FACTORY.build("database_error", cause);
	}

	public static AnnotatorException buildExternalDatabaseException(Throwable cause) {
		return buildExternalDatabaseException(cause, null);
	}

	public static AnnotatorException buildExternalDatabaseException(Throwable cause, String comment) {
		return EXCEPTION_FACTORY.build("external_database_error", comment, null, cause);
	}

	public static AnnotatorException buildExternalServiceException(Throwable cause) {
		return EXCEPTION_FACTORY.build("external_service_error", cause);
	}

	public static AnnotatorException buildExternalServiceException(Throwable cause, String serviceName, String comment) {
		return EXCEPTION_FACTORY.build("external_service_error", comment, new HashMap<String, Object>() {{
			put("service_name", serviceName);
		}}, cause);
	}

	public static AnnotatorException buildExternalServiceException(Throwable cause, String comment) {
		return EXCEPTION_FACTORY.build("external_service_error", comment, null, cause);
	}

	public static AnnotatorException buildNotMultipartRequestException() {
		return EXCEPTION_FACTORY.build("not_multipart_request");
	}

	public static AnnotatorException buildFileNotUploadException() {
		return EXCEPTION_FACTORY.build("file_not_upload");
	}

	public static AnnotatorException buildIOErrorException(IOException e) {
		return EXCEPTION_FACTORY.build("io_error", e);
	}

	public static AnnotatorException buildOperationException(Throwable cause) {
		return EXCEPTION_FACTORY.build("operation_error", cause);
	}

	public static AnnotatorException buildInvalidCredentialsException() {
		return EXCEPTION_FACTORY.build("invalid_credentials");
	}

	public static AnnotatorException buildInvalidVepJsonException(Throwable cause) {
		return EXCEPTION_FACTORY.build("operation_error", cause);
	}

	public static AnnotatorException buildLargeVcfFile(int maxSize) {
		return EXCEPTION_FACTORY.build("large_vcf_file", new HashMap<String, Object>() {{
			put("max_size", maxSize);
		}});
	}

	public static AnnotatorException buildInvalidVcfFile(Throwable cause) {
		return EXCEPTION_FACTORY.build("invalid_vcf_file", cause);
	}

	public static AnnotatorException buildNotEqualSamplesVcfAndCnvFile() {
		return EXCEPTION_FACTORY.build("not_equal_samples_vcf_and_cnv_file");
	}

	public static AnnotatorException buildInvalidChromosome(String value) {
		return EXCEPTION_FACTORY.build("invalid_chromosome", new HashMap<String, Object>() {{
			put("value", value);
		}});
	}

	public static AnnotatorException buildNotUniqueValueException(String fieldName, Object fieldValue) {
		return EXCEPTION_FACTORY.build("not_unique_value", new HashMap<String, Object>() {{
			put("field_name", fieldName);
			put("field_value", fieldValue);
		}});
	}

	public static AnnotatorException buildInvalidValueException(String fieldName) {
		return EXCEPTION_FACTORY.build("invalid_value", new HashMap<String, Object>() {{
			put("field_name", fieldName);
		}});
	}

	public static AnnotatorException buildInvalidValueException(String fieldName, Object fieldValue) {
		return buildInvalidValueException(fieldName, fieldValue, null);
	}

	public static AnnotatorException buildInvalidValueException(String fieldName, Object fieldValue, String comment) {
		return EXCEPTION_FACTORY.build("invalid_value", comment, new HashMap<String, Object>() {{
			put("field_name", fieldName);
			put("field_value", fieldValue);
		}});
	}

	public static AnnotatorException buildInvalidValueJsonException(String fieldName, Throwable cause) {
		return EXCEPTION_FACTORY.build("invalid_value_json", null, new HashMap<String, Object>() {{
			put("field_name", fieldName);
		}}, cause);
	}

	public static AnnotatorException buildInvalidJsonException(ParseException cause) {
		return EXCEPTION_FACTORY.build("invalid_json", null, null, cause);
	}

	public static AnnotatorException buildInvalidInventoryException(Throwable cause) {
		return EXCEPTION_FACTORY.build("invalid_inventory", null, null, cause);
	}

	public static AnnotatorException buildInvalidValueInventoryException(String fieldName) {
		return buildInvalidValueInventoryException(fieldName, null);
	}

    public static AnnotatorException buildInvalidValueInventoryException(String fieldName, String comment) {
        return EXCEPTION_FACTORY.build("invalid_value_inventory", comment, new HashMap<String, Object>() {{
            put("field_name", fieldName);
        }}, null);
    }

	public static AnnotatorException buildInvalidOperation(String comment) {
		return EXCEPTION_FACTORY.build("invalid_operation", comment);
	}

	public static AnnotatorException buildServerTimeoutException() {
		return EXCEPTION_FACTORY.build("server_timeout");
	}

	public static AnnotatorException buildServerBusyException(String cause) {
		return EXCEPTION_FACTORY.build("server_busy", Collections.singletonMap("cause", cause));
	}

	public static AnnotatorException buildServerOverloadedException() {
		return EXCEPTION_FACTORY.build("server_overloaded");
	}

	public static AnnotatorException buildServerShutsDownException() {
		return EXCEPTION_FACTORY.build("server_shuts_down");
	}
}


