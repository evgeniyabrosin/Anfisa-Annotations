package ru.processtech.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import ru.processtech.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class GnomadConfigConfigConnector extends DatabaseConfigConnector {

	public GnomadConfigConfigConnector(JSONObject parse) {
		super(parse);
	}
}
