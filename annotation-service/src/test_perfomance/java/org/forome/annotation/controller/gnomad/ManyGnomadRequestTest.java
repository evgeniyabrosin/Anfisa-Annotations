package org.forome.annotation.controller.gnomad;

import org.forome.annotation.controller.THttpClient;
import org.forome.annotation.utils.ExecutorServiceUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ManyGnomadRequestTest {

	private final static Logger log = LoggerFactory.getLogger(ManyGnomadRequestTest.class);

	@Test
	public void test() throws Exception {
		Path fileRequest = Paths.get(
				getClass().getClassLoader().getResource("many_gnomad_requests.json").toURI()
		);
		List<String> fileLines = Files.readAllLines(fileRequest);
		String data = String.join("", fileLines);

		CompletableFuture<Long>[] futures = new CompletableFuture[100];
		for (int i = 0; i < futures.length; i++) {
			CompletableFuture<Long> future = new CompletableFuture<Long>();
			futures[i] = future;
			int finalI = i;
			ExecutorServiceUtils.poolExecutor.execute(() -> {
				long t1 = System.currentTimeMillis();

				try {
					THttpClient tHttpClient = new THttpClient("http://anfisa.forome.org/annotationservice/");
					tHttpClient.logon("admin", "b82nfGl5sdg");
					tHttpClient.execute("GetGnomAdData", new HashMap<String, String>() {{
						put("data", data);
					}});
					long t2 = System.currentTimeMillis();
					future.complete(t2 - t1);
				} catch (Throwable e) {
					log.error("Exception, {} end {}", finalI, System.currentTimeMillis(), e);
					future.completeExceptionally(e);
				}
			});

			Thread.sleep(i);
		}

		long[] times = new long[futures.length];
		for (int i = 0; i < futures.length; i++) {
			long time = futures[i].get();
			times[i] = time;
			log.debug("future: {}, time {}", i, time);
		}

		//Вычисляем медиану
		Arrays.sort(times);
		double median;
		if (times.length % 2 == 0) {
			median = ((double) times[times.length / 2] + (double) times[times.length / 2 - 1]) / 2;
		} else {
			median = (double) times[times.length / 2];
		}
		log.debug("min: {}, median: {}, max: {}", times[0], median, times[times.length - 1]);
	}
}
