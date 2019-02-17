package ru.processtech.forome.annotation.executionqueue;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;
import com.infomaximum.database.exception.DatabaseException;
import ru.processtech.forome.annotation.exception.ExceptionBuilder;
import ru.processtech.forome.annotation.exception.ServiceException;

public class EditableResource<T extends DomainObject & DomainObjectEditable> extends ReadableResource<T> {

	EditableResource(Class<T> tClass) {
		super(tClass);
	}

	public T create(ExecutionTransaction transaction) throws ServiceException {
		try {
			return transaction.getDBTransaction().create(tClass);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public void save(T newObj, ExecutionTransaction transaction) throws ServiceException {
		try {
			transaction.getDBTransaction().save(newObj);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}

	public void remove(T obj, ExecutionTransaction transaction) throws ServiceException {
		try {
			transaction.getDBTransaction().remove(obj);
		} catch (DatabaseException e) {
			throw ExceptionBuilder.buildDatabaseException(e);
		}
	}


}
