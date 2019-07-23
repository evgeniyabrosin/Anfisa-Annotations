package org.forome.annotation.executionqueue;

import org.forome.annotation.exception.AnnotatorException;

public abstract class Execution<T> {

	public abstract void prepare(ResourceProvider resources);

	public ExecutionQueue.Priority getPriority() {
		return ExecutionQueue.Priority.HIGH;
	}

	public String getMaintenanceMarker() {
		return null;
	}

	public abstract T execute(ExecutionTransaction transaction) throws AnnotatorException;
}
