package org.forome.annotation.network;

import org.forome.annotation.network.exception.NetworkException;
import org.forome.annotation.network.session.SessionService;
import org.forome.annotation.network.transport.HttpTransport;
import org.forome.annotation.network.transport.builder.HttpBuilderTransport;
import org.forome.annotation.network.transport.builder.connector.BuilderHttpConnector;
import org.forome.annotation.network.transport.jetty.ServerErrorHandler;
import org.forome.annotation.network.transport.spring.SpringConfigurationMvc;

import java.time.Duration;

public class NetworkService {

	public final SessionService sessionService;

	private HttpTransport httpTransport;

	public NetworkService(int port, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.sessionService = new SessionService();

		SpringConfigurationMvc.init(Duration.ofMinutes(30), null, uncaughtExceptionHandler);
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
