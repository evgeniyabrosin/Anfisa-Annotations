/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.connector.liftover;

import htsjdk.samtools.liftover.LiftOver;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;

import java.io.*;

public class LiftoverConnector implements AutoCloseable {

	private final File fileSampleHg19toHg38;
	private final LiftOver liftOverHg19toHg38;

	private final File fileSampleHg38toHg19;
	private final LiftOver liftOverHg38toHg19;

	public LiftoverConnector() throws IOException {
		try (InputStream inputFileSample = getClass().getClassLoader().getResourceAsStream("hg19ToHg38.over.chain.gz")) {
			fileSampleHg19toHg38 = File.createTempFile("hg19ToHg38-", ".over.chain.gz");
			try (OutputStream out = new FileOutputStream(fileSampleHg19toHg38)) {
				int read;
				byte[] bytes = new byte[1024];
				while ((read = inputFileSample.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			}
			fileSampleHg19toHg38.deleteOnExit();
		}
		liftOverHg19toHg38 = new LiftOver(fileSampleHg19toHg38);
		liftOverHg19toHg38.setShouldLogFailedIntervalsBelowThreshold(false);

		try (InputStream inputFileSample = getClass().getClassLoader().getResourceAsStream("hg38ToHg19.over.chain.gz")) {
			fileSampleHg38toHg19 = File.createTempFile("hg38ToHg19-", ".over.chain.gz");
			try (OutputStream out = new FileOutputStream(fileSampleHg38toHg19)) {
				int read;
				byte[] bytes = new byte[1024];
				while ((read = inputFileSample.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			}
			fileSampleHg38toHg19.deleteOnExit();
		}
		liftOverHg38toHg19 = new LiftOver(fileSampleHg38toHg19);
		liftOverHg38toHg19.setShouldLogFailedIntervalsBelowThreshold(false);
	}

	public Interval toHG38(Interval intervalHg19) {
		int start = Math.min(intervalHg19.start, intervalHg19.end);
		int end = Math.max(intervalHg19.start, intervalHg19.end);

		htsjdk.samtools.util.Interval interval = liftOverHg19toHg38.liftOver(new htsjdk.samtools.util.Interval(
				intervalHg19.chromosome.toString(),
				start,
				end
		));
		if (interval == null) return null;

		if (intervalHg19.start <= intervalHg19.end) {
			return new Interval(intervalHg19.chromosome, interval.getStart(), interval.getEnd());
		} else {
			return new Interval(intervalHg19.chromosome, interval.getEnd(), interval.getStart());
		}
	}

	public Position toHG19(Position positionHg38) {
		htsjdk.samtools.util.Interval interval = liftOverHg38toHg19.liftOver(new htsjdk.samtools.util.Interval(
				positionHg38.chromosome.toString(),
				positionHg38.value,
				positionHg38.value
		));
		if (interval == null) return null;
		return new Position(positionHg38.chromosome, interval.getStart());
	}

	@Override
	public void close() {
		try {
			fileSampleHg19toHg38.delete();
		} catch (Throwable ignore) {
		}
		try {
			fileSampleHg38toHg19.delete();
		} catch (Throwable ignore) {
		}
	}
}
