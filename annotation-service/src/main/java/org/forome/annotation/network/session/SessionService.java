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

package org.forome.annotation.network.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.forome.annotation.database.entityobject.user.UserReadable;

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
