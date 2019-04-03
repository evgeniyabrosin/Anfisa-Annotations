package org.forome.annotation.network.transport.builder;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.forome.annotation.network.transport.builder.connector.BuilderHttpConnector;
import org.forome.annotation.network.transport.builder.filter.BuilderFilter;

import java.util.HashSet;
import java.util.Set;

public class HttpBuilderTransport {

	private Set<BuilderHttpConnector> builderConnectors;

	private Class classWebMvcConfig;
	private ErrorHandler errorHandler;

	private Set<BuilderFilter> filters;

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


	public HttpBuilderTransport addFilter(BuilderFilter filterItem) {
		if (filters == null) {
			filters = new HashSet<BuilderFilter>();
		}
		filters.add(filterItem);
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

	public Set<BuilderFilter> getFilters() {
		return filters;
	}
}
