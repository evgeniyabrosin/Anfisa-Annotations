package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class HgmdConfigConfigConnector extends DatabaseConfigConnector {

	public HgmdConfigConfigConnector(JSONObject parse) {
		super(parse);
	}
}
