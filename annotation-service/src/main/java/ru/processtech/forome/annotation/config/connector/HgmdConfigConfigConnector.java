package ru.processtech.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import ru.processtech.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class HgmdConfigConfigConnector extends DatabaseConfigConnector {

	public HgmdConfigConfigConnector(JSONObject parse) {
		super(parse);
	}
}
