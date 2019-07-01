package org.forome.annotation.config;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.config.connector.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ServiceConfig {

	public final EnsemblVepConfigConnector ensemblVepConfigConnector;
	public final GnomadConfigConnector gnomadConfigConnector;
	public final ClinVarConfigConnector clinVarConfigConnector;
	public final HgmdConfigConnector hgmdConfigConnector;
	public final GTFConfigConnector gtfConfigConnector;
	public final SpliceAIConfigConnector spliceAIConfigConnector;


	public ServiceConfig() throws Exception {
		this(Paths.get("config.json").toAbsolutePath());
	}

	public ServiceConfig(Path configFile) throws Exception {
		if (!Files.exists(configFile)) {
			throw new RuntimeException("File: " + configFile.toString() + " not found");
		}
		JSONObject configFileJson;
		try (InputStream is = Files.newInputStream(configFile, StandardOpenOption.READ)) {
			configFileJson = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(is);
		}

		JSONObject jConnectors = (JSONObject) configFileJson.get("connectors");
		ensemblVepConfigConnector = new EnsemblVepConfigConnector((JSONObject) jConnectors.get("ensembl-vep"));
		gnomadConfigConnector = new GnomadConfigConnector((JSONObject) jConnectors.get("gnomad"));
		clinVarConfigConnector = new ClinVarConfigConnector((JSONObject) jConnectors.get("clinvar"));
		hgmdConfigConnector = new HgmdConfigConnector((JSONObject) jConnectors.get("hgmd"));
		gtfConfigConnector = new GTFConfigConnector((JSONObject) jConnectors.get("gtf"));
		spliceAIConfigConnector = new SpliceAIConfigConnector((JSONObject) jConnectors.get("spliceai"));
	}

}
