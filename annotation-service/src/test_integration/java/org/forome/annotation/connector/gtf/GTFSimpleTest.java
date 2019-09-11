package org.forome.annotation.connector.gtf;

import org.forome.annotation.connector.gtf.struct.GTFRegion;
import org.forome.annotation.connector.gtf.struct.GTFResultLookup;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class GTFSimpleTest extends GTFBaseTest {

	@Test
	public void testByChromosomeAndPositions() throws Exception {
		String chromosome = "5";
		String transcript = "ENST00000282356";
		long position = 110694251;

		GTFRegion expectedGtfRegion = gtfConnector.getRegion(
				transcript,
				position
		).get();


		List<GTFResultLookup> lookups = gtfConnector
				.getRegionByChromosomeAndPositions(chromosome, new long[] {position}).get();
		GTFResultLookup actualGTFResultLookup = lookups.stream()
				.filter(gtfResultLookup -> transcript.equals(gtfResultLookup.transcript)).findFirst().get();

		Assert.assertEquals(expectedGtfRegion.region, actualGTFResultLookup.region);
		Assert.assertEquals(expectedGtfRegion.indexRegion, actualGTFResultLookup.index);
	}
}
