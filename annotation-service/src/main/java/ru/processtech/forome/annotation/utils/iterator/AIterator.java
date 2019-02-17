package ru.processtech.forome.annotation.utils.iterator;

import ru.processtech.forome.annotation.exception.ServiceException;

public interface AIterator<T> extends AutoCloseable {

	boolean hasNext() throws ServiceException;

	T next() throws ServiceException;

	void close() throws ServiceException;
}
