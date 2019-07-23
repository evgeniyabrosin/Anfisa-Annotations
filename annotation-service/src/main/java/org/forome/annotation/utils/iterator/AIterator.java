package org.forome.annotation.utils.iterator;

import org.forome.annotation.exception.AnnotatorException;

public interface AIterator<T> extends AutoCloseable {

	boolean hasNext() throws AnnotatorException;

	T next() throws AnnotatorException;

	void close() throws AnnotatorException;
}
