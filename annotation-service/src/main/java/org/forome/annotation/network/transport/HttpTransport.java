package org.forome.annotation.network.transport;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.forome.annotation.network.exception.NetworkException;
import org.forome.annotation.network.transport.builder.HttpBuilderTransport;
import org.forome.annotation.network.transport.builder.connector.BuilderHttpConnector;
import org.forome.annotation.network.transport.builder.filter.BuilderFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.ArrayList;
import java.util.List;

public class HttpTransport {

	private final static Logger log = LoggerFactory.getLogger(HttpTransport.class);

	private final Server server;

	public HttpTransport(final HttpBuilderTransport httpBuilderTransport) throws NetworkException {
		server = new Server(new QueuedThreadPool(10000));
		server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", -1);

		if (httpBuilderTransport.getBuilderConnectors() == null || httpBuilderTransport.getBuilderConnectors().isEmpty()) {
			throw new NetworkException("Not found connectors");
		}
		List<Connector> connectors = new ArrayList<>();
		for (BuilderHttpConnector builderHttpConnector : httpBuilderTransport.getBuilderConnectors()) {
			Connector connector = builderHttpConnector.build(server);
			connectors.add(connector);
		}
		server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");


		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(httpBuilderTransport.getClassWebMvcConfig());
		context.addServlet(new ServletHolder("default", new DispatcherServlet(applicationContext)), "/");

		//Возможно есть регистрируемые фильтры
		if (httpBuilderTransport.getFilters() != null) {
			for (BuilderFilter builderFilter : httpBuilderTransport.getFilters()) {
				context.addFilter(builderFilter.filterClass, builderFilter.pathSpec, builderFilter.dispatches);
			}
		}

		HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(new Handler[]{ context, new DefaultHandler() });
		server.setHandler(handlers);

		if (httpBuilderTransport.getErrorHandler() != null)
			server.setErrorHandler(httpBuilderTransport.getErrorHandler());

		try {
			server.start();
		} catch (Exception e) {
			throw new NetworkException(e);
		}
	}

}
