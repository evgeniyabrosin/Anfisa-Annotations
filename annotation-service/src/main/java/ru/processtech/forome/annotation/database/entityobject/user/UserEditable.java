package ru.processtech.forome.annotation.database.entityobject.user;

import com.infomaximum.database.domainobject.DomainObjectEditable;
import ru.processtech.forome.annotation.utils.Random;

public class UserEditable extends UserReadable implements DomainObjectEditable {

	public UserEditable(long id) {
		super(id);
	}

	public void setLogin(String login) {
		set(FIELD_LOGIN, login);
	}

	public void setPassword(String password) {
		byte[] salt = null;
		if (password != null) {
			salt = new byte[16];
			Random.secureRandom.nextBytes(salt);
		}
		set(FIELD_SALT, salt);
		set(FIELD_PASSWORD_HASH, getSaltyPasswordHash(password, salt));
	}

}
