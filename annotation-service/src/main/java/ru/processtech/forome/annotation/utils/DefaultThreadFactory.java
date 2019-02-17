package ru.processtech.forome.annotation.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {

	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);

	public DefaultThreadFactory(String factoryName, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.group = new DefaultThreadGroup(factoryName, uncaughtExceptionHandler);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r,
				group.getName() + "-t-" + threadNumber.getAndIncrement(),
				0);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}

	private static class DefaultThreadGroup extends ThreadGroup {

		private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

		DefaultThreadGroup(String name, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
			super(name);

			this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			uncaughtExceptionHandler.uncaughtException(t, e);
		}
	}
}
