package org.forome.annotation.struct.authcontext.usersession;

import org.forome.annotation.struct.authcontext.AuthorizedContext;

public class UserSessionAuthContext extends AuthorizedContext {

    private final long userId;
    private final String sessionId;

    public UserSessionAuthContext(long userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }
}
