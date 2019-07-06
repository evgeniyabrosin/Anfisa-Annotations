package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class ClinVarConfigConnector extends DatabaseConfigConnector {

	public ClinVarConfigConnector(JSONObject parse) {
		super(parse);
	}
}
