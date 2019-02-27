package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class GnomadConfigConfigConnector extends DatabaseConfigConnector {

	public GnomadConfigConfigConnector(JSONObject parse) {
		super(parse);
	}
}
