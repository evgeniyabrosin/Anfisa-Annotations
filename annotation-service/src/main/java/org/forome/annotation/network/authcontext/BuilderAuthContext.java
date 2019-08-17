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
