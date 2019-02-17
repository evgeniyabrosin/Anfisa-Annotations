package ru.processtech.forome.annotation.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceUtils {

	public static final ExecutorService poolExecutor = Executors.newCachedThreadPool();
}
