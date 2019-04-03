package org.forome.annotation.network.component;

import com.infomaximum.database.domainobject.filter.HashFilter;
import org.forome.annotation.executionqueue.ExecutionTransaction;
import org.forome.annotation.executionqueue.ReadableResource;
import org.forome.annotation.database.entityobject.user.UserReadable;
import org.forome.annotation.executionqueue.ResourceProvider;

import java.util.Arrays;

public class AuthComponent {

	private final ReadableResource<UserReadable> userReadableResource;

	public AuthComponent(ResourceProvider resources) {
		userReadableResource = resources.getReadableResource(UserReadable.class);
	}

	public UserReadable auth(String login, String password, ExecutionTransaction transaction) {
		UserReadable user = userReadableResource.find(new HashFilter(UserReadable.FIELD_LOGIN, login), transaction);
		if (user == null) return null;

		byte[] countSaltyPasswordHash = UserReadable.getSaltyPasswordHash(password, user.getSalt());
		if (Arrays.equals(countSaltyPasswordHash, user.getPasswordHash())) {
			return user;
		} else {
			return null;
		}
	}
}
