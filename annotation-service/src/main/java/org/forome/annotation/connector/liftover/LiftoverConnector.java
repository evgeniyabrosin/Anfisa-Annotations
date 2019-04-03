package org.forome.annotation.connector.liftover;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;

import java.io.*;

public class LiftoverConnector {

	private final LiftOver liftOver;

	public LiftoverConnector() throws IOException {
		File fileSample;
		try (InputStream inputFileSample = getClass().getClassLoader().getResourceAsStream("hg19ToHg38.over.chain.gz")) {
			fileSample = File.createTempFile("hg19ToHg38-", ".over.chain.gz");
			try (OutputStream out = new FileOutputStream(fileSample)) {
				int read;
				byte[] bytes = new byte[1024];
				while ((read = inputFileSample.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			}
			fileSample.deleteOnExit();
		}

		liftOver = new LiftOver(fileSample);
	}

	public Integer hg38(String chromosome, long position) {
		String ch = String.format("chr%s", chromosome.toUpperCase());
		Interval interval = liftOver.liftOver(new Interval(ch, (int) (position), (int) (position)));
		if (interval == null) return null;
		return interval.getStart();
	}
}
