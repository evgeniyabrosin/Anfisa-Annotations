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
import org.forome.annotation.network.transport.builder.BuilderHttpConnector;
import org.forome.annotation.network.transport.builder.HttpBuilderTransport;
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
