package org.forome.annotation.executionqueue.iterator;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.exception.DatabaseException;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.utils.iterator.AIterator;

public class IteratorEntity<E extends DomainObject> implements AIterator<E> {

	private final com.infomaximum.database.domainobject.iterator.IteratorEntity<E> ie;

	public IteratorEntity(com.infomaximum.database.domainobject.iterator.IteratorEntity<E> ie) {
		this.ie = ie;
	}

	@Override
	public boolean hasNext() {
		return ie.hasNext();
	}

	@Override
	public E next() throws AnnotatorException {
		try {
			return ie.next();
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	@Override
	public void close() throws AnnotatorException {
		try {
			ie.close();
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}
}
