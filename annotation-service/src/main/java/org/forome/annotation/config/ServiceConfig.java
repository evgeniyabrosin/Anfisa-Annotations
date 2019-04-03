package org.forome.annotation.config;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.config.connector.ClinVarConfigConfigConnector;
import org.forome.annotation.config.connector.GTFConfigConfigConnector;
import org.forome.annotation.config.connector.GnomadConfigConfigConnector;
import org.forome.annotation.config.connector.HgmdConfigConfigConnector;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ServiceConfig {

	public final Path dataPath;

	public final GnomadConfigConfigConnector gnomadConfigConnector;
	public final ClinVarConfigConfigConnector clinVarConfigConnector;
	public final HgmdConfigConfigConnector hgmdConfigConnector;
	public final GTFConfigConfigConnector gtfConfigConnector;

	public ServiceConfig() throws Exception {

		dataPath = Paths.get("data");
		if (!Files.exists(dataPath)) {
			Files.createDirectory(dataPath);
		}

		Path configFile = dataPath.resolve("config.json").toAbsolutePath();
		if (!Files.exists(configFile)) {
			throw new RuntimeException("File: " + configFile.toString() + " not found");
		}
		JSONObject configFileJson;
		try (InputStream is = Files.newInputStream(configFile, StandardOpenOption.READ)) {
			configFileJson = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(is);
		}

		JSONObject jConnectors = (JSONObject) configFileJson.get("connectors");
		gnomadConfigConnector = new GnomadConfigConfigConnector((JSONObject) jConnectors.get("gnomad"));
		clinVarConfigConnector = new ClinVarConfigConfigConnector((JSONObject) jConnectors.get("clinvar"));
		hgmdConfigConnector = new HgmdConfigConfigConnector((JSONObject) jConnectors.get("hgmd"));
		gtfConfigConnector = new GTFConfigConfigConnector((JSONObject) jConnectors.get("gtf"));

	}

}
