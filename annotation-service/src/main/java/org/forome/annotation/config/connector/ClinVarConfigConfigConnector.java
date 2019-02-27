package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class ClinVarConfigConfigConnector extends DatabaseConfigConnector {

	public ClinVarConfigConfigConnector(JSONObject parse) {
		super(parse);
	}
}
