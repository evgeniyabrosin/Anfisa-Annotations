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
			builderConnectors = new HashSet<>();
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
