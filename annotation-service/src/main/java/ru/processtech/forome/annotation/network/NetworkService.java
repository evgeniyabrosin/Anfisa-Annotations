package ru.processtech.forome.annotation.network;

import ru.processtech.forome.annotation.network.exception.NetworkException;
import ru.processtech.forome.annotation.network.session.SessionService;
import ru.processtech.forome.annotation.network.transport.HttpTransport;
import ru.processtech.forome.annotation.network.transport.builder.HttpBuilderTransport;
import ru.processtech.forome.annotation.network.transport.builder.connector.BuilderHttpConnector;
import ru.processtech.forome.annotation.network.transport.jetty.ServerErrorHandler;
import ru.processtech.forome.annotation.network.transport.spring.SpringConfigurationMvc;

import java.time.Duration;

public class NetworkService {

	public final SessionService sessionService;

	private HttpTransport httpTransport;

	public NetworkService(int port, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.sessionService = new SessionService();

		SpringConfigurationMvc.init(Duration.ofMinutes(10), null, uncaughtExceptionHandler);
		try {
			httpTransport = new HttpTransport(
					new HttpBuilderTransport(SpringConfigurationMvc.class)
							.addConnector(
									new BuilderHttpConnector(port)
							)
							.withErrorHandler(new ServerErrorHandler(uncaughtExceptionHandler))
			);
		} catch (NetworkException e) {
			uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
		}
	}


}
