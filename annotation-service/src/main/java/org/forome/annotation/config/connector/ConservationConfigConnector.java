package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class ConservationConfigConnector extends DatabaseConfigConnector {

    public ConservationConfigConnector(JSONObject parse) {
        super(parse);
    }
}
