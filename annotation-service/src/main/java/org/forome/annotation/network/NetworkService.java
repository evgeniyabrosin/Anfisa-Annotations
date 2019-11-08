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
