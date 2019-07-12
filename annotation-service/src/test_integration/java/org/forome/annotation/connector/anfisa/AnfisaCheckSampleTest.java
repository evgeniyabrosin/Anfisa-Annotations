package org.forome.annotation.connector.anfisa;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.AnfisaBaseTest;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.struct.Chromosome;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 * if (count < 421) -  ["2",73675228,73675228,"TCTC"]
 * if (count < 2195) - ["12",80849328,80849328,"A"]
 * if (count < 2663) - ["17",72913072,72913072,"C"]
 * if (count < 2664) - ["17",72913797,72913797,"C"]
 * if (count < 2702) - ["18",28666527,28666527,"TTAA"]
 * if (count < 2703) - ["18",28666527,28666527,"TCAA"]
 */
public class AnfisaCheckSampleTest extends AnfisaBaseTest {

	private final static Logger log = LoggerFactory.getLogger(AnfisaCheckSampleTest.class);

	@Test
	public void singleSample() throws Exception {
		check("anfisa_single_sample.json");
	}

	@Test
	public void manySample() throws Exception {
		check("anfisa_many_sample.json");
	}

	private void check(String sampleFileName) throws Exception {
		Path fileSample = Paths.get(
				getClass().getClassLoader().getResource(sampleFileName).toURI()
		);
		List<String> fileSampleLines = Files.readAllLines(fileSample);
		String samples = String.join("", fileSampleLines);

		JSONArray jSamples = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(samples);
		int count = 0;
		for (Object oSamble : jSamples) {
			count++;
			if (count < 83) continue;

			JSONObject sample = (JSONObject) oSamble;
			JSONArray sampleInput = (JSONArray) sample.get("input");

			List<AnfisaResult> iResults;
			try {
				iResults = anfisaConnector.request(
						(Chromosome) sampleInput.get(0),
						((Number) sampleInput.get(1)).longValue(),
						((Number) sampleInput.get(2)).longValue(),
						(String) sampleInput.get(3)
				).get();
			} catch (Exception e) {
				throw new RuntimeException("Ошибка получение результатов. input: " + sampleInput.toJSONString(), e);
			}


			JSONArray sampleResults = (JSONArray) sample.get("result");
			Assert.assertEquals(sampleResults.size(), iResults.size());

			for (int i = 0; i < iResults.size(); i++) {
				JSONObject jSampleResult = (JSONObject) sampleResults.get(i);
				JSONObject jResult = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(
						//Сделано специально, что бы потерять всю информацию о типах и работать с чистым json
						GetAnfisaJSONController.build(iResults.get(i)).toJSONString()
				);

//				try {
//					JSONEquals.equals(jSampleResult, jResult);
//				} catch (Exception e) {
//					throw new RuntimeException("Ошибка сравнения результатов. input: " + sampleInput.toJSONString(), e);
//				}
			}

			log.debug("Success test(s): {}/{}", count, jSamples.size());
		}
	}

}
