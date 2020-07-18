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

import net.minidev.json.JSONObject;
import org.forome.annotation.iterator.vcf.VCFFileIterator;
import org.forome.annotation.iterator.vepjson.VepJsonFileIterator;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.mavariant.MAVariant;
import org.forome.annotation.struct.mavariant.MAVariantCNV;
import org.forome.annotation.struct.mavariant.MAVariantVCF;
import org.forome.annotation.struct.mavariant.MAVariantVep;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ThreadExecutor implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(ThreadExecutor.class);

	private final int index;

	private final EnsemblVepService ensemblVepService;
	private final Processing processing;

	private final MCase samples;

	private final int start;
	private final int step;

	private final VCFFileIterator vcfFileIterator;
	private final VepJsonFileIterator vepJsonIterator;

	private Result nextResult;
	private final Deque<Result> waitExecuteVariants;//Варианты ожидающие выполнения

	private int nextPosition;
	private volatile boolean isCompleted = false;

	public ThreadExecutor(
			int index,
			EnsemblVepService ensemblVepService,
			Processing processing,
			MCase samples,
			Path pathVcf, Path pathVepJson,
			Path cnvFile,
			int start, int step,
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler
	) {
		this.index = index;

		this.ensemblVepService = ensemblVepService;
		this.processing = processing;

		this.samples = samples;

		this.start = start;
		this.step = step;

		this.vcfFileIterator = new VCFFileIterator(pathVcf, cnvFile);

		if (pathVepJson != null) {
			vepJsonIterator = new VepJsonFileIterator(pathVepJson);
		} else {
			vepJsonIterator = null;
		}

		this.nextResult = new Result(nextPosition, new CompletableFuture<>());
		this.waitExecuteVariants = new ConcurrentLinkedDeque<>();
		this.waitExecuteVariants.add(nextResult);

		nextPosition = start + step;

		//Исполнитель
		Thread executor = new Thread(() -> {
			log.debug("Thread: {} start", index);

			Source source = null;
			try {
				//Прокручиваем до начала итерации
				if (start > 0) {
					nextSource(start);
				}
				source = nextSource(1);
			} catch (NoSuchElementException e) {
				isCompleted = true;
				log.debug("Thread: {} completed", index);
			}

			while (true) {

				//TODO Переписать на засыпание потока
				while (waitExecuteVariants.isEmpty()) {
					if (isCompleted) return;
					try {
						Thread.sleep(10L);
					} catch (InterruptedException e) {
					}
				}
				Result result = waitExecuteVariants.poll();
				if (isCompleted) {
					result.future.complete(null);
					continue;
				}

				MAVariant maVariant = source.variant;

				if (maVariant instanceof MAVariantVCF && vepJsonIterator != null) {
					List<ProcessingResult> processingResults = processing.exec(samples, maVariant);

					result.future.complete(processingResults);
				} else {
//					String alternative;
//					if (variant instanceof MAVariantVCF) {
//						VariantContext variantContext = ((MAVariantVCF) variant).variantContext;
//
//						Allele allele = variantContext.getAlternateAlleles().stream()
//								.filter(iAllele -> !iAllele.getDisplayString().equals("*"))
//								.max(Comparator.comparing(variantContext::getCalledChrCount))
//								.orElse(null);
//						alternative = allele.getDisplayString();
//					} else if (variant instanceof MAVariantCNV) {
//						alternative = "-";
//					} else {
//						throw new RuntimeException("Not support type variant: " + variant);
//					}

					Variant variant;
					if (maVariant instanceof MAVariantCNV) {
						variant = ((MAVariantCNV) maVariant).variantCNV;
					} else {
						throw new RuntimeException("Not support type maVariant: " + maVariant);
					}

					ensemblVepService.getVepJson(variant)
							.thenApply(iVepJson -> {
								((VariantVep) variant).setVepJson(iVepJson);

								ProcessingResult processingResult = processing.exec(samples, variant);
								result.future.complete(Collections.singletonList(processingResult));
								return null;
							})
							.exceptionally(throwable -> {
								result.future.completeExceptionally(throwable);
								return null;
							});
				}

				//Дожидаемся выполнения
				try {
					result.future.join();
				} catch (Throwable ignore) {
				}

				try {
					source = nextSource(step);
				} catch (NoSuchElementException e) {
					isCompleted = true;
					log.debug("Thread: {} completed", index);
				}
			}
		});
		executor.setUncaughtExceptionHandler(uncaughtExceptionHandler);
		executor.start();
	}

	private Source nextSource(int step) {
		if (step < 1) throw new IllegalArgumentException();
		MAVariantVep variantVep = null;
		JSONObject vepJson = null;
		for (int i = 0; i < step; i++) {
			try {
				variantVep = vcfFileIterator.next();
			} catch (NoSuchElementException ne) {
				//Валидация того, что в vep.json - тоже не осталось записей
				if (vepJsonIterator != null) {
					try {
						vepJsonIterator.next();
						throw new RuntimeException("Not equals count rows, vcf file and vep.json file");
					} catch (NoSuchElementException ignore) {
					}
				}
				throw ne;
			}
			if (variantVep instanceof MAVariantVCF && vepJsonIterator != null) {
				try {
					vepJson = vepJsonIterator.next();
				} catch (NoSuchElementException ne) {
					//Валидация того, что в vep.json - остались записи
					throw new RuntimeException("Not equals count rows, vcf file and vep.json file");
				}
			} else {
				vepJson = null;
			}
		}
		return new Source(variantVep, vepJson);
	}

	public Result next() {
		Result value;
		synchronized (waitExecuteVariants) {
			value = nextResult;

			nextPosition += step;
			if (isCompleted) {
				nextResult = new Result(nextPosition, CompletableFuture.completedFuture(null));
			} else {
				nextResult = new Result(nextPosition, new CompletableFuture<>());
				this.waitExecuteVariants.add(nextResult);
			}
		}
		return value;
	}


	@Override
	public void close() throws IOException {
		vcfFileIterator.close();

		if (vepJsonIterator != null) {
			vepJsonIterator.close();
		}
	}
}
