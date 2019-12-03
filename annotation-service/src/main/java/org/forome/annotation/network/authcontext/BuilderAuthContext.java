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

package org.forome.annotation.network.authcontext;

import org.forome.annotation.Service;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.authcontext.AuthorizedContext;
import org.forome.annotation.struct.authcontext.apikey.ApiKeyAuthContext;
import org.forome.annotation.struct.authcontext.usersession.UserSessionAuthContext;

import javax.servlet.http.HttpServletRequest;

public class BuilderAuthContext {

	private final Service service;

	public BuilderAuthContext(Service service) {
		this.service = service;
	}

	public AuthorizedContext auth(HttpServletRequest request) throws AnnotatorException  {
		String sessionId = request.getParameter("session");
		if (sessionId != null) {
			Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
			if (userId == null) {
				throw ExceptionBuilder.buildInvalidCredentialsException();
			}
			return new UserSessionAuthContext(userId, sessionId);
		}

		String apikey = request.getParameter("apikey");
		if (apikey != null) {
			if (apikey.equals(service.getServiceConfig().frontendConfig.apikey)) {
				return new ApiKeyAuthContext();
			}
		}

		throw ExceptionBuilder.buildInvalidCredentialsException();
	}
}
