package org.forome.annotation.network;

import org.forome.annotation.network.exception.NetworkException;
import org.forome.annotation.network.session.SessionService;
import org.forome.annotation.network.transport.HttpTransport;
import org.forome.annotation.network.transport.builder.BuilderHttpConnector;
import org.forome.annotation.network.transport.builder.HttpBuilderTransport;
import org.forome.annotation.network.transport.jetty.ServerErrorHandler;
import org.forome.annotation.network.transport.spring.SpringConfigurationMvc;

import java.time.Duration;

public class NetworkService {

	public final SessionService sessionService;

	private HttpTransport httpTransport;

	public NetworkService(int port, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) throws NetworkException {
		this.sessionService = new SessionService();

		SpringConfigurationMvc.init(Duration.ofMinutes(30));

		httpTransport = new HttpTransport(
				new HttpBuilderTransport(SpringConfigurationMvc.class)
						.addConnector(
								new BuilderHttpConnector(port)
						)
						.withErrorHandler(new ServerErrorHandler(uncaughtExceptionHandler))
		);
	}


}
