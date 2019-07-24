package org.forome.annotation.executionqueue;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.filter.EmptyFilter;
import com.infomaximum.database.domainobject.filter.Filter;
import com.infomaximum.database.exception.DatabaseException;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.executionqueue.iterator.IteratorEntity;

public class ReadableResource<T extends DomainObject> {

	protected final Class<T> tClass;

	ReadableResource(Class<T> tClass) {
		this.tClass = tClass;
	}

	public Class<T> getDomainClass() {
		return tClass;
	}

	public T get(long id, ExecutionTransaction transaction) throws AnnotatorException {
		try {
			return transaction.getDBTransaction().get(tClass, id, null);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public T find(final Filter filter, ExecutionTransaction transaction) throws AnnotatorException {
		try (com.infomaximum.database.domainobject.iterator.IteratorEntity<T> iter = transaction.getDBTransaction().find(tClass, filter, null)) {
			return iter.hasNext() ? iter.next() : null;
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public IteratorEntity<T> iterator(ExecutionTransaction transaction) throws AnnotatorException {
		try {
			return new IteratorEntity<>(transaction.getDBTransaction().find(tClass, EmptyFilter.INSTANCE, null));
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public IteratorEntity<T> findAll(final Filter filter, ExecutionTransaction transaction) throws AnnotatorException {
		try {
			return new IteratorEntity<>(transaction.getDBTransaction().find(tClass, filter, null));
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

}
