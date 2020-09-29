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

package org.forome.annotation.controller;

import com.google.common.base.Strings;
import htsjdk.tribble.TribbleException;
import htsjdk.variant.vcf.VCFFileReader;
import io.reactivex.Observable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.Main;
import org.forome.annotation.Service;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.format.FormatAnfisaHttpClient;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.network.authcontext.BuilderAuthContext;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.struct.CasePlatform;
import org.forome.annotation.struct.variant.Variant;
import org.forome.core.struct.Assembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping(value = {"/FormatVcf", "/annotationservice/FormatVcf"})
public class FormatVcfController {

	private final static Logger log = LoggerFactory.getLogger(FormatVcfController.class);

	@RequestMapping(value = {"/get"})
	public CompletableFuture<ResponseEntity> get(HttpServletRequest request) {
		Service service = Service.getInstance();

		BuilderAuthContext builderAuthContext = new BuilderAuthContext(service);
		builderAuthContext.auth(request);

		String requestId = UUID.randomUUID().toString().toLowerCase();
		log.debug("FormatVcfController requestId: {}, time: {}", requestId, System.currentTimeMillis());

		EnsemblVepService ensemblVepService = service.getEnsemblVepService();
		if (ensemblVepService == null) {
			throw ExceptionBuilder.buildInvalidOperation("inited");
		}

		AnfisaConnector anfisaConnector = service.getAnfisaConnector();
		if (anfisaConnector == null) {
			throw ExceptionBuilder.buildInvalidOperation("inited");
		}

		Processing processing = new Processing(anfisaConnector, TypeQuery.PATIENT_HG19);

		TempVCFFile tempVCFFile = buildTempVCFFile(request);

		Annotator annotator;
		try {
			annotator = new Annotator(
					ensemblVepService, processing,
					String.format("%s_wgs", "noname"), CasePlatform.WGS,
					Assembly.GRCh37,
					null,null,null,
					tempVCFFile.path, null
			);
		} catch (IOException e) {
			throw ExceptionBuilder.buildIOErrorException(e);
		} catch (ParseException e) {
			throw ExceptionBuilder.buildInvalidJsonException(e);
		}

		FormatAnfisaHttpClient formatAnfisaHttpClient;
		try {
			formatAnfisaHttpClient = new FormatAnfisaHttpClient();
		} catch (IOException e) {
			throw ExceptionBuilder.buildIOErrorException(e);
		}

		AnnotatorResult annotatorResult = annotator.exec(
				null,0
		);

		CompletableFuture<ResponseEntity> completableFuture = new CompletableFuture<>();
		List<JSONObject> ourResults = Collections.synchronizedList(new ArrayList<JSONObject>());
		annotatorResult.observableAnfisaResult
				.map(processingResult -> {
					log.debug("FormatVcfController requestId: {}, 1: {}", requestId, processingResult);
					return processingResult;
				})
				.flatMap(processingResult ->
						Observable.fromFuture(formatAnfisaHttpClient.request(processingResult.toJSON().toJSONString())
								.thenApply(jsonArray -> {
									log.debug("FormatVcfController requestId: {}, 2: {}", requestId, jsonArray);
									return new Object[]{processingResult, jsonArray};
								}).exceptionally(throwable -> {
									Main.crash(throwable);
									return null;
								})
						)
				)
				.map(objects -> {
					ProcessingResult processingResult = (ProcessingResult) objects[0];
					JSONArray results = (JSONArray) objects[1];
					log.debug("FormatVcfController requestId: {}, 3: {}", requestId, results.toJSONString().length());

					JSONObject out = new JSONObject();
					Variant variant = processingResult.variant;
					out.put("input", new JSONArray() {{
						add(variant.chromosome.getChromosome());
						add(variant.getStart());
						add(variant.end);
						add(processingResult.variant.getStrAlt());
					}});
					out.put("result", new JSONArray() {{
						add(results);
					}});
					return out;
				})
				.subscribe(jsonArray -> {
					log.debug("FormatVcfController requestId: {}, 4: {}", requestId, jsonArray.toJSONString().length());
					ourResults.add(jsonArray);
				}, throwable -> {
					log.error("Exception execute request", throwable);
					completableFuture.completeExceptionally(throwable);
					tempVCFFile.close();
				}, () -> {
					log.debug("FormatVcfController requestId: {}, 5", requestId);
					JSONArray out = new JSONArray();
					for (JSONObject jsonObject : ourResults) {
						out.add(jsonObject);
					}
					completableFuture.complete(ResponseBuilder.build(out));
					log.debug("FormatVcfController build response, time: {}", System.currentTimeMillis());
					tempVCFFile.close();
				});
		return completableFuture;
	}

	private static class TempVCFFile implements AutoCloseable {

		public final VCFFileReader vcfFileReader;
		public final Path path;

		public TempVCFFile(VCFFileReader vcfFileReader, Path path) {
			this.vcfFileReader = vcfFileReader;
			this.path = path;
		}

		@Override
		public void close() {
			vcfFileReader.close();
			try {
				Files.deleteIfExists(path);
			} catch (IOException ignore) {
			}
		}
	}

	private static TempVCFFile buildTempVCFFile(HttpServletRequest request) {
		Path vcfFile = null;
		try {
			vcfFile = Files.createTempFile("temp_", ".vcf");

			String pData = request.getParameter("data");
			if (!Strings.isNullOrEmpty(pData)) {
				Files.write(vcfFile, pData.getBytes(StandardCharsets.UTF_8));
			} else {
				if (!(request instanceof MultipartHttpServletRequest)) {
					throw ExceptionBuilder.buildNotMultipartRequestException();
				}
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

				Map.Entry<String, List<MultipartFile>> entry = multipartRequest.getMultiFileMap().entrySet().stream().findFirst().orElse(null);
				if (entry == null) {
					throw ExceptionBuilder.buildFileNotUploadException();
				}
				MultipartFile multipartFile = entry.getValue().stream().findFirst().orElse(null);
				if (multipartFile == null) {
					throw ExceptionBuilder.buildFileNotUploadException();
				}

				multipartFile.transferTo(vcfFile.toFile());
			}

			return new TempVCFFile(
					new VCFFileReader(vcfFile, false),
					vcfFile
			);
		} catch (IOException e) {
			throw ExceptionBuilder.buildIOErrorException(e);
		} catch (TribbleException.MalformedFeatureFile e) {
			throw ExceptionBuilder.buildInvalidVcfFile(e);
		} catch (Throwable e) {
			try {
				if (vcfFile != null) {
					Files.deleteIfExists(vcfFile);
				}
			} catch (IOException ignore) {
			}
			throw e;
		}
	}
}
