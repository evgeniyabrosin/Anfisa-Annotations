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
import htsjdk.samtools.util.Interval;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Position;

import java.io.*;

public class LiftoverConnector implements AutoCloseable {

	private final File fileSample;
	private final LiftOver liftOver;

	public LiftoverConnector() throws IOException {
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
		liftOver.setShouldLogFailedIntervalsBelowThreshold(false);
	}

	public Position<Integer> toHG38(Chromosome chromosome, Position<Integer> position) {
		int start = Math.min(position.start, position.end);
		int end = Math.max(position.start, position.end);

		Interval interval = liftOver.liftOver(new Interval(
				chromosome.toString(),
				start,
				end
		));
		if (interval == null) return null;

		if (position.start <= position.end) {
			return new Position<>(interval.getStart(), interval.getEnd());
		} else {
			return new Position<>(interval.getEnd(), interval.getStart());
		}
	}

	@Override
	public void close() {
		try {
			fileSample.delete();
		} catch (Throwable ignore) {
		}
	}
}
