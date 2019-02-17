package ru.processtech.forome.annotation.utils.function;

import ru.processtech.forome.annotation.exception.ServiceException;

@FunctionalInterface
public interface Consumer<T> {

	void accept(T t) throws ServiceException;
}