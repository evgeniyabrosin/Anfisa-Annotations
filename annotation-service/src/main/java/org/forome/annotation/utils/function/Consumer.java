package org.forome.annotation.utils.function;

import org.forome.annotation.exception.AnnotatorException;

@FunctionalInterface
public interface Consumer<T> {

	void accept(T t) throws AnnotatorException;
}