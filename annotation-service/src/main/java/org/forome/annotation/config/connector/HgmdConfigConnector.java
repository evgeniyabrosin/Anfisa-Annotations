package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class HgmdConfigConnector extends DatabaseConfigConnector {

	public HgmdConfigConnector(JSONObject parse) {
		super(parse);
	}
}
