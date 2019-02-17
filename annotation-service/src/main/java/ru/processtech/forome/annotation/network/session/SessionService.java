package ru.processtech.forome.annotation.network.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ru.processtech.forome.annotation.database.entityobject.user.UserReadable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SessionService {

	public static final Duration SESSION_TIMEOUT = Duration.ofMinutes(15);

	private final Cache<Object, Object> sessions;

	public SessionService() {
		this.sessions = CacheBuilder.newBuilder()
				.expireAfterAccess(SESSION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
				.build();
	}

	public String buildSession(UserReadable userReadable) {
		String sessionId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
		sessions.put(sessionId, userReadable.getId());
		return sessionId;
	}

	public Long checkSession(String sessionId) {
		return (Long) sessions.getIfPresent(sessionId);
	}
}
