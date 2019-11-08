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
