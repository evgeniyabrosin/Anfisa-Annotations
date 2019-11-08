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

import java.util.concurrent.*;

public class DefaultThreadPoolExecutor extends ThreadPoolExecutor {

	private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	public DefaultThreadPoolExecutor(
			int corePoolSize,
			int maximumPoolSize,
			long keepAliveTime,
			TimeUnit unit,
			BlockingQueue<Runnable> workQueue,
			String threadFactoryName,
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new DefaultThreadFactory(threadFactoryName, uncaughtExceptionHandler));
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (uncaughtExceptionHandler == null) {
			return;
		}

		if (t == null) {
			try {
				Future<?> future = (Future<?>) r;
				if (future.isDone()) {
					future.get();
				}
			} catch (ExecutionException e) {
				t = e.getCause();
			} catch (Throwable e) {
				t = e;
			}
		}

		if (t != null) {
			uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
		}
	}
};
