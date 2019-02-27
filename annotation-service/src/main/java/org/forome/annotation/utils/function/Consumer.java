package org.forome.annotation.utils.function;

import org.forome.annotation.exception.ServiceException;

@FunctionalInterface
public interface Consumer<T> {

	void accept(T t) throws ServiceException;
}