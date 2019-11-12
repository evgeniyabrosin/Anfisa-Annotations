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

package org.forome.annotation.annotator.executor;

import htsjdk.variant.vcf.VCFFileReader;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.mcase.MCase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AnnotatorExecutor implements AutoCloseable {

	private final ThreadExecutor[] threadExecutors;

	private int activeExecutor;

	public AnnotatorExecutor(
			EnsemblVepService ensemblVepService,
			Processing processing,
			String caseSequence, MCase mCase,
			Path pathVcf, Path pathVepJson,
			Path cnvFile,
			int start, int thread,
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler
	) {
		if (thread < 1) throw new IllegalArgumentException();

		//Validation samples fam-file and vcf-file
		VCFFileReader vcfFileReader = new VCFFileReader(pathVcf, false);
		List<String> vcfSamples = vcfFileReader.getFileHeader().getGenotypeSamples();
		if (vcfSamples.size() != mCase.samples.size() || !vcfSamples.containsAll(mCase.samples.keySet())) {
			throw ExceptionBuilder.buildNotEqualSamplesVcfAndFamFile();
		}

		threadExecutors = new ThreadExecutor[thread];
		for (int i = 0; i < thread; i++) {
			threadExecutors[i] = new ThreadExecutor(
					i + 1,
					ensemblVepService,
					processing,
					caseSequence, mCase,
					pathVcf, pathVepJson,
					cnvFile,
					start + i, thread,
					uncaughtExceptionHandler
			);
		}

		activeExecutor = 0;
	}

	public synchronized Result next() {
		ThreadExecutor threadExecutor = threadExecutors[activeExecutor];
		activeExecutor++;
		if (activeExecutor > threadExecutors.length - 1) {
			activeExecutor = 0;
		}

		return threadExecutor.next();
	}

	@Override
	public void close() throws IOException {
		for (ThreadExecutor threadExecutor : threadExecutors) {
			threadExecutor.close();
		}
	}
}
