/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.utils;

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
