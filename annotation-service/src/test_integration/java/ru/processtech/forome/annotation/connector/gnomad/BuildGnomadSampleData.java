package ru.processtech.forome.annotation.connector.gnomad;

import org.junit.Test;

public class BuildGnomadSampleData {

	@Test
	public void build() throws Exception {
//		Path fileRequest = Paths.get(
//				getClass().getClassLoader().getResource("many_gnomad_requests.json").toURI()
//		);
//		List<String> fileLines = Files.readAllLines(fileRequest);
//		String data = String.join("", fileLines);
//
//		THttpClient tHttpClient = new THttpClient("http://anfisa.forome.org/annotationservice/");
////		THttpClient tHttpClient = new THttpClient("http://localhost:8095/");
//		tHttpClient.logon("admin", "b82nfGl5sdg");
//
//		String response = tHttpClient.execute("GetGnomAdData", new HashMap<String, String>(){{
//			put("data", data);
//		}});
//
//		JSONObject jResponse = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(response);
//		JSONArray jDataResponse = (JSONArray) jResponse.get("data");
//
//		Path fileGnomadSampleData = Paths.get("gnomad_sample_data.json");
//		Files.write(fileGnomadSampleData, jDataResponse.toJSONString().getBytes(StandardCharsets.UTF_8));
	}
}
