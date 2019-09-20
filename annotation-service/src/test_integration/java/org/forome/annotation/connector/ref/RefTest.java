package org.forome.annotation.connector.ref;

import org.forome.annotation.struct.Chromosome;
import org.junit.Assert;
import org.junit.Test;

public class RefTest extends RefBaseTest {

	@Test
	public void test() {
		Assert.assertEquals("A", refConnector.getRef(Chromosome.of("1"), 33475750, 33475750));
		Assert.assertEquals("AC", refConnector.getRef(Chromosome.of("1"), 33476224, 33476225));
		Assert.assertEquals("CAT", refConnector.getRef(Chromosome.of("1"), 103471457, 103471459));
		Assert.assertEquals("C", refConnector.getRef(Chromosome.of("10"), 123357561, 123357561));
		Assert.assertEquals("G", refConnector.getRef(Chromosome.of("11"), 727446, 727446));
		Assert.assertEquals("GCT", refConnector.getRef(Chromosome.of("11"), 832983, 832985));
	}
}
