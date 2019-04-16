package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class GnomadConfigConnector extends DatabaseConfigConnector {

	public GnomadConfigConnector(JSONObject parse) {
		super(parse);
	}
}
