package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class GTFConfigConnector extends DatabaseConfigConnector {

	public GTFConfigConnector(JSONObject parse) {
		super(parse);
	}
}
