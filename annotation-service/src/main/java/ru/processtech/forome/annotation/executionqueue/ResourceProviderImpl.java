package ru.processtech.forome.annotation.executionqueue;

import com.infomaximum.database.anotation.Entity;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;
import com.infomaximum.database.exception.runtime.ClosedObjectException;

import java.util.HashMap;

public class ResourceProviderImpl implements ResourceProvider, AutoCloseable {

	private final HashMap<String, ExecutionQueue.LockType> resources = new HashMap<>();
	private boolean closed = false;

	protected ResourceProviderImpl() {
	}

	@Override
	public <T extends DomainObject & DomainObjectEditable> EditableResource<T> getEditableResource(Class<T> resClass) {
		borrowResource(resolveReadClass(resClass), ExecutionQueue.LockType.EXCLUSIVE);
		return new EditableResource<>(resClass);
	}

	@Override
	public <T extends DomainObject> ReadableResource<T> getReadableResource(Class<T> resClass) {
		checkReadClass(resClass);
		borrowResource(resClass, ExecutionQueue.LockType.SHARED);
		return new ReadableResource<>(resClass);
	}

	@Override
	public void borrowResource(Class resClass, ExecutionQueue.LockType type) {
		borrowResource(resClass.getName(), type);
	}

	public void borrowResource(String resource, ExecutionQueue.LockType type) {
		check();
		appendResource(resource, type, resources);
	}

	protected HashMap<String, ExecutionQueue.LockType> getResources() {
		check();
		return resources;
	}

	@Override
	public void close() {
		closed = true;
	}

	private void check() {
		if (closed) {
			throw new ClosedObjectException(this.getClass());
		}
	}

	private <T extends DomainObject> void checkReadClass(Class<T> resClass) {
		if (!resClass.isAnnotationPresent(Entity.class)) {
			throw new IllegalArgumentException("class-Readable " + resClass.getSimpleName() + " not contains Entity annotation");
		}
	}

	public static void appendResource(String resource, ExecutionQueue.LockType type, HashMap<String, ExecutionQueue.LockType> destination) {
		destination.merge(resource, type, (val1, val2) -> val1 == ExecutionQueue.LockType.EXCLUSIVE ? val1 : val2);
	}

	private static <T extends DomainObject & DomainObjectEditable> Class<?> resolveReadClass(Class<T> editClass) {
		Class<?> readClass = editClass;
		do {
			readClass = readClass.getSuperclass();
		} while (!readClass.isAnnotationPresent(Entity.class));
		return readClass;
	}
}
