package ru.processtech.forome.annotation.executionqueue;


import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;

public interface ResourceProvider {

	<T extends DomainObject> ReadableResource<T> getReadableResource(Class<T> resClass);

	<T extends DomainObject & DomainObjectEditable> EditableResource<T> getEditableResource(Class<T> resClass);

	void borrowResource(Class resClass, ExecutionQueue.LockType type);
}
