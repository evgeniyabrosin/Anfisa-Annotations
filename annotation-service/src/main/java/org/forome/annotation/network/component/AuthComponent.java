package org.forome.annotation.network.component;

import com.infomaximum.database.domainobject.filter.HashFilter;
import com.infomaximum.querypool.QueryTransaction;
import com.infomaximum.querypool.ReadableResource;
import com.infomaximum.querypool.ResourceProvider;
import org.forome.annotation.database.entityobject.user.UserReadable;

import java.util.Arrays;

public class AuthComponent {

	private final ReadableResource<UserReadable> userReadableResource;

	public AuthComponent(ResourceProvider resources) {
		userReadableResource = resources.getReadableResource(UserReadable.class);
	}

	public UserReadable auth(String login, String password, QueryTransaction transaction) {
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
