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

package org.forome.annotation.annotator;

import io.reactivex.Observable;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.annotator.executor.AnnotatorExecutor;
import org.forome.annotation.annotator.executor.Result;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.annotator.struct.AnnotatorResultMetadata;
import org.forome.annotation.annotator.utils.CaseUtils;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.CasePlatform;
import org.forome.annotation.struct.mcase.MCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//curl "localhost:8290/get?array=hg38&loc=12:885081&alt=G"


public class Annotator {

	private final static Logger log = LoggerFactory.getLogger(Annotator.class);

	private final EnsemblVepService ensemblVepService;
	private final Processing processing;

	private final String caseName;
	private final CasePlatform casePlatform;

	private final MCase mCase;

	private final Path pathVcf;
	private final Path pathVepJson;

	public Annotator(
			EnsemblVepService ensemblVepService,
			Processing processing,

			String caseName,
			CasePlatform casePlatform,
			Assembly assembly,

			Path pathFam,
			Path pathFamSampleName,
			Path pathCohorts,
			Path pathVcf,
			Path pathVepJson

	) throws IOException, ParseException {
		this.ensemblVepService = ensemblVepService;
		this.processing = processing;

		this.caseName = caseName;
		this.casePlatform = casePlatform;

		this.pathVcf = pathVcf;
		if (!Files.exists(pathVcf)) {
			throw new RuntimeException("Vcf file is not exists: " + pathVcf.toAbsolutePath());
		}
		if (!pathVcf.getFileName().toString().endsWith(".vcf")) {
			throw new IllegalArgumentException("Bad name vcf file (Need *.vcf): " + pathVcf.toAbsolutePath());
		}

		if (!Files.exists(pathFam)) {
			throw new RuntimeException("Fam file is not exists: " + pathFam.toAbsolutePath());
		}
		if (!pathFam.getFileName().toString().endsWith(".fam")) {
			throw new IllegalArgumentException("Bad name fam file: " + pathFam.toAbsolutePath());
		}

		this.pathVepJson=pathVepJson;

		try (InputStream isFam = Files.newInputStream(pathFam);
			 InputStream isFamSampleName = (pathFamSampleName != null) ? Files.newInputStream(pathFamSampleName) : null;
			 InputStream isCohorts = (pathCohorts != null) ? Files.newInputStream(pathCohorts) : null
		) {
			this.mCase = CaseUtils.parseFamFile(assembly, isFam, isFamSampleName, isCohorts);
		}
	}

	public AnnotatorResultMetadata buildMetadata() {
		String caseSequence = String.format("%s_%s", caseName, casePlatform.name().toLowerCase());

		return new AnnotatorResultMetadata(caseSequence, pathVcf, mCase, processing.getAnfisaConnector());
	}

	public AnnotatorResult exec(
			Path cnvFile,
			int startPosition
	) {
		return new AnnotatorResult(
				Observable.create(o -> {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try (AnnotatorExecutor annotatorExecutor = new AnnotatorExecutor(
									ensemblVepService, processing,
									mCase,
									pathVcf, pathVepJson,
									cnvFile,
									startPosition, getThreads(mCase),
									(t, e) -> o.tryOnError(e)
							)) {
								boolean run = true;
								while (run) {
									Result result = annotatorExecutor.next();
									List<ProcessingResult> processingResults;
									try {
										processingResults = result.future.get();
										if (processingResults != null) {
											for (ProcessingResult processingResult : processingResults) {
												o.onNext(processingResult);
											}
										} else {
											run = false;
										}
									} catch (Throwable e) {
										log.error("throwable", e);
									}
								}
								o.onComplete();
							} catch (Throwable e) {
								o.tryOnError(e);
							}
						}
					}).start();
				})
		);
	}


	/**
	 * Необходимо учитывать, что при большом колличестве samples у нас сильно увеличивается потребления оперативной памяти
	 * для этого мы усеньшаем кол-во поток и соответсвенно кол-во экземпляторов чтения vcf-файлов
	 */
	private static int getThreads(MCase mCase) {
		int maxThread = Runtime.getRuntime().availableProcessors() * 2;

		int ks = mCase.samples.size() / 100;
		int thread = maxThread / (ks + 1);
		if (thread == 0) thread = 1;

		return thread;
	}
}
