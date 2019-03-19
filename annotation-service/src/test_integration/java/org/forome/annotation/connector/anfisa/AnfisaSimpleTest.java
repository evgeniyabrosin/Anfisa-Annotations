package org.forome.annotation.connector.anfisa;

import org.forome.annotation.AnfisaBaseTest;
import org.junit.Test;

public class AnfisaSimpleTest extends AnfisaBaseTest {

	@Test
	public void test() throws Exception {
//		anfisaConnector.request("1", 6484880, 6484880,"G").get();
//		anfisaConnector.request("1", 6500660, 6500660,"A").get();
//		anfisaConnector.request("1", 6501044, 6501044,"G").get();
//      anfisaConnector.request("1", 12040324, 12040324,"G").get();
		anfisaConnector.request("1", 12065841, 12065841,"T").get();
//		anfisaConnector.request("1", 16351275, 16351275,"G").get();

//		anfisaConnector.request("1", 12065841, 12065841,"T").get().forEach(anfisaResult -> {
//			GetAnfisaJSONController.build(anfisaResult).toJSONString();
//		});
	}
}
