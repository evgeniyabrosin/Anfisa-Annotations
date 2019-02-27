package org.forome.annotation.connector.gnomad;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GnomadSampleTest extends GnomadBaseTest {

	private final static Logger log = LoggerFactory.getLogger(GnomadSampleTest.class);

	private static Pattern PATTERN_URL = Pattern.compile(
			"^http://gnomad[.]broadinstitute[.]org/variant/(.*)-([0-9]+)-(.*)-(.*)$"
	);

	@Test
	public void test() throws Exception {
		Path fileSample = Paths.get(
				getClass().getClassLoader().getResource("gnomad_sample_data.json").toURI()
		);
		List<String> fileSampleLines = Files.readAllLines(fileSample);
		String samples = String.join("", fileSampleLines);

		JSONArray jSamples = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(samples);
		int count = 0;
		for (Object oSample : jSamples) {
			count++;

			JSONObject sample = (JSONObject) oSample;

			JSONArray inputSample = (JSONArray) sample.get("input");

			String chromosome = (String) inputSample.get(0);
			long position = ((Number) inputSample.get(1)).longValue();
			String reference = (String) inputSample.get(2);
			String alternative = (String) inputSample.get(3);

			GnomadResult expected = build((JSONObject) sample.get("gnomAD"));

			GnomadResult actual = gnomadConnector.request(chromosome, position, reference, alternative).join();

			Assert.assertEquals(expected, actual);
			log.debug("Success test(s): {}", count);
		}
	}

	private static GnomadResult build(JSONObject value) {
		if (!value.containsKey("overall")) {
			return GnomadResult.EMPTY;
		}

		GnomadResult.Sum exomes = null;
		if (value.containsKey("exomes")) {
			JSONObject jExomes = (JSONObject) value.get("exomes");
			exomes = new GnomadResult.Sum(
					jExomes.getAsNumber("AN").longValue(),
					jExomes.getAsNumber("AC").longValue(),
					jExomes.getAsNumber("AF").doubleValue()
			);
		}

		GnomadResult.Sum genomes = null;
		if (value.containsKey("genomes")) {
			JSONObject jGenomes = (JSONObject) value.get("genomes");
			genomes = new GnomadResult.Sum(
					jGenomes.getAsNumber("AN").longValue(),
					jGenomes.getAsNumber("AC").longValue(),
					jGenomes.getAsNumber("AF").doubleValue()
			);
		}

		JSONObject jOverall = (JSONObject) value.get("overall");
		GnomadResult.Sum overall = new GnomadResult.Sum(
				jOverall.getAsNumber("AN").longValue(),
				jOverall.getAsNumber("AC").longValue(),
				jOverall.getAsNumber("AF").doubleValue()
		);

		String popmax = value.getAsString("popmax");
		double popmaxAF = value.getAsNumber("popmax_af").doubleValue();
		long popmaxAN = value.getAsNumber("popmax_an").longValue();

		Set<GnomadResult.Url> urls = new HashSet<>();
		for (Object oUrl : (JSONArray) value.get("url")) {
			String url = (String) oUrl;

			Matcher matcher = PATTERN_URL.matcher(url);
			if (!matcher.matches()) {
				throw new RuntimeException("Not matcher url");
			}

			String urlChromosome = matcher.group(1);
			long urlPosition = Integer.parseInt(matcher.group(2));
			String urlReference = matcher.group(3);
			String urlAlternative = matcher.group(4);

			urls.add(
					new GnomadResult.Url(
							urlChromosome,
							urlPosition,
							urlReference,
							urlAlternative
					)
			);
		}

		return new GnomadResult(
				exomes, genomes, overall,
				popmax, popmaxAF, popmaxAN,
				urls
		);
	}
}
