package org.forome.annotation.exception;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.querypool.ExceptionBuilder;

public class QueryPoolExceptionBuilder implements ExceptionBuilder {

	@Override
	public RuntimeException buildDatabaseException(DatabaseException e) {
		return org.forome.annotation.exception.ExceptionBuilder.buildDatabaseException(e);
	}

	@Override
	public RuntimeException buildServerBusyException(String cause) {
		return org.forome.annotation.exception.ExceptionBuilder.buildServerBusyException(cause);
	}

	@Override
	public RuntimeException buildServerOverloadedException() {
		return org.forome.annotation.exception.ExceptionBuilder.buildServerOverloadedException();
	}

	@Override
	public RuntimeException buildServerShutsDownException() {
		return org.forome.annotation.exception.ExceptionBuilder.buildServerShutsDownException();
	}
}
