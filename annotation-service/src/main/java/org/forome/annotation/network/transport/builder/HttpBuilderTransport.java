package org.forome.annotation.network.transport.builder;

import org.eclipse.jetty.server.handler.ErrorHandler;

import java.util.HashSet;
import java.util.Set;

public class HttpBuilderTransport {

	private Set<BuilderHttpConnector> builderConnectors;

	private Class classWebMvcConfig;
	private ErrorHandler errorHandler;

	public HttpBuilderTransport(Class classWebMvcConfig) {
		this.classWebMvcConfig = classWebMvcConfig;
	}

	public HttpBuilderTransport addConnector(BuilderHttpConnector builderConnector) {
		if (builderConnectors == null) {
			builderConnectors = new HashSet<BuilderHttpConnector>();
		}
		builderConnectors.add(builderConnector);
		return this;
	}

	public HttpBuilderTransport withErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		return this;
	}

	public Set<BuilderHttpConnector> getBuilderConnectors() {
		return builderConnectors;
	}

	public Class getClassWebMvcConfig() {
		return classWebMvcConfig;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

}
