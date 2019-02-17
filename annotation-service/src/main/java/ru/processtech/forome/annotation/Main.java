package ru.processtech.forome.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.processtech.forome.annotation.utils.ArgumentParser;

import java.util.concurrent.FutureTask;

public class Main {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		ArgumentParser arguments;
		try {
			arguments = new ArgumentParser(args);
		} catch (Throwable e) {
			log.error("Exception arguments parser", e);
			System.exit(2);
			return;
		}

		try {
			Service service = new Service(
					arguments,
					(thread, throwable) -> crash(throwable)
			);

			FutureTask<Void> stopSignal = new FutureTask<>(() -> null);
			Runtime.getRuntime().addShutdownHook(new Thread(stopSignal, "shutDownHook"));
			stopSignal.get();

			service.stop();
		} catch (Throwable e) {
			crash(e);
		}
	}

	public static void crash(Throwable e) {
		log.error("Application crashing ", e);
		System.exit(1);
	}
}
