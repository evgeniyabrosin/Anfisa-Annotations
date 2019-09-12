package org.forome.annotation.connector.beacon;

public class BeaconConnector {

	public static final String BASE_URL = "https://beacon-network.org/";

	public static String getUrl(
			String chromosome,
			long position,
			String ref,
			String alt
	) {
		return String.format(
				BASE_URL + "#/search?pos=%s&chrom=%s&allele=%s&ref=%s&rs=GRCh37",
				position, chromosome, alt, ref
		);
	}

}
