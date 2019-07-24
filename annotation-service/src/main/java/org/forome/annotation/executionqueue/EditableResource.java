package org.forome.annotation.executionqueue;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;
import com.infomaximum.database.exception.DatabaseException;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;

public class EditableResource<T extends DomainObject & DomainObjectEditable> extends ReadableResource<T> {

	EditableResource(Class<T> tClass) {
		super(tClass);
	}

	public T create(ExecutionTransaction transaction) throws AnnotatorException {
		try {
			return transaction.getDBTransaction().create(tClass);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public void save(T newObj, ExecutionTransaction transaction) throws AnnotatorException {
		try {
			transaction.getDBTransaction().save(newObj);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public void remove(T obj, ExecutionTransaction transaction) throws AnnotatorException {
		try {
			transaction.getDBTransaction().remove(obj);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}


}
