package org.forome.annotation.connector.gtf;

import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.config.connector.GTFConfigConfigConnector;
import org.forome.annotation.connector.gtf.struct.GTFResult;
import org.forome.annotation.utils.DefaultThreadPoolExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GTFConnector {

	private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

	private final DatabaseConnector databaseConnector;
	private final GTFDataConnector gtfDataConnector;

	private final ExecutorService threadPoolGTFExecutor;

	public GTFConnector(
			GTFConfigConfigConnector gtfConfigConnector,
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler
	) throws Exception {
		this.databaseConnector = new DatabaseConnector(gtfConfigConnector);
		this.gtfDataConnector = new GTFDataConnector(databaseConnector);
		threadPoolGTFExecutor = new DefaultThreadPoolExecutor(
				MAX_THREAD_COUNT,
				MAX_THREAD_COUNT,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				"GnomadExecutorQueue",
				uncaughtExceptionHandler
		);
	}

	public CompletableFuture<GTFResult> request(String chromosome, long position) {
		CompletableFuture<GTFResult> future = new CompletableFuture();
		threadPoolGTFExecutor.submit(() -> {
			try {
				GTFResult result = gtfDataConnector.getGene(chromosome, position);
				future.complete(result);
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
}
