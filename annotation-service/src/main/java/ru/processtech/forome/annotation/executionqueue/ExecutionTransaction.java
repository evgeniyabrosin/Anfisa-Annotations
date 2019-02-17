package ru.processtech.forome.annotation.executionqueue;

import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.domainobject.Transaction;
import com.infomaximum.database.exception.DatabaseException;
import ru.processtech.forome.annotation.exception.ExceptionBuilder;
import ru.processtech.forome.annotation.exception.ServiceException;

public class ExecutionTransaction implements AutoCloseable {

	private final Transaction transaction;

	ExecutionTransaction(DomainObjectSource domainObjectSource) {
		transaction = domainObjectSource.buildTransaction();
	}

	public Transaction getDBTransaction() {
		return transaction;
	}

	void commit() throws ServiceException {
		try {
			transaction.commit();
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	@Override
	public void close() {
		try {
			transaction.close();
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}
}
