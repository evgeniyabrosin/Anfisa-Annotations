/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.custominput;

import com.google.common.base.Strings;
import org.apache.commons.cli.*;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnector;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

public class CustomInputMain {

	private final static Logger log = LoggerFactory.getLogger(CustomInputMain.class);

	public static final String OPTION_FILE_CONFIG = "config";

	public static final String OPTION_FILE_INPUT = "input";

	private static ServiceConfig serviceConfig;
	private static NotificationService notificationService;
	private static SSHConnectService sshTunnelService;
	private static DatabaseConnectService databaseConnectService;

	private static GnomadConnector gnomadConnector;
	private static SpliceAIConnector spliceAIConnector;
	private static ConservationData conservationConnector;
	private static HgmdConnector hgmdConnector;
	private static ClinvarConnector clinvarConnector;
	private static LiftoverConnector liftoverConnector;
	private static GTFConnector gtfConnector;
	private static GTEXConnector gtexConnector;
	private static PharmGKBConnector pharmGKBConnector;
	private static EnsemblVepService ensemblVepService;
	private static AnfisaConnector anfisaConnector;
	private static Processing processing;

	public static void main(String[] args) {
		Options options = new Options()

				.addOption(Option.builder()
						.longOpt(OPTION_FILE_CONFIG)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to config file")
						.build())

				.addOption(Option.builder()
						.longOpt(OPTION_FILE_INPUT)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to input file")
						.build());

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);

			String strConfigFile = cmd.getOptionValue(OPTION_FILE_CONFIG);
			if (Strings.isNullOrEmpty(strConfigFile)) {
				throw new IllegalArgumentException("Missing config file");
			}
			Path config = Paths.get(strConfigFile).toAbsolutePath();
			if (!Files.exists(config)) {
				throw new IllegalArgumentException("Config file is not exists: " + config);
			}

			String strInventoryFile = cmd.getOptionValue(OPTION_FILE_INPUT);
			if (Strings.isNullOrEmpty(strInventoryFile)) {
				throw new IllegalArgumentException("Missing inventory file");
			}
			Path pathInputFile = Paths.get(strInventoryFile).toAbsolutePath();
			if (!Files.exists(pathInputFile)) {
				throw new IllegalArgumentException("Inventory file is not exists: " + config);
			}


			serviceConfig = new ServiceConfig(config);

			if (serviceConfig.notificationSlackConfig != null) {
				notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
			}

			sshTunnelService = new SSHConnectService();
			databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
//            gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, arguments));
			gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e));
			spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);
			conservationConnector = new ConservationData(databaseConnectService, serviceConfig.conservationConfigConnector);
			hgmdConnector = new HgmdConnector(databaseConnectService, serviceConfig.hgmdConfigConnector);
			clinvarConnector = new ClinvarConnector(databaseConnectService, serviceConfig.clinVarConfigConnector);
			liftoverConnector = new LiftoverConnector();
			gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> fail(e));
			gtexConnector = new GTEXConnector(databaseConnectService, serviceConfig.gtexConfigConnector);
			pharmGKBConnector = new PharmGKBConnector(databaseConnectService, serviceConfig.pharmGKBConfigConnector);
			ensemblVepService = new EnsemblVepExternalService((t, e) -> fail(e));
			anfisaConnector = new AnfisaConnector(
					gnomadConnector,
					spliceAIConnector,
					conservationConnector,
					hgmdConnector,
					clinvarConnector,
					liftoverConnector,
					gtfConnector,
					gtexConnector,
					pharmGKBConnector
			);
			processing = new Processing(anfisaConnector, TypeQuery.WIDE_HG19);


			//Парсим файл
			ArrayList<GetAnfisaJSONController.RequestItem> requestItems = new ArrayList<>();
			List<String> lines = Files.readAllLines(pathInputFile);

			ArrayList<String> requestIds = new ArrayList<>();

			//Формат
			//rs749334
			//SNV:T>G
			//17:21411973

			int l = 0;
			while (l < lines.size()) {
				String l0 = lines.get(l);
				l++;
				if (!l0.startsWith("rs")) {
					throw new RuntimeException("bad line: " + lines.get(l));
				}

				VariantType variantType;
				String ref;
				String[] alts;
				if (lines.get(l).contains(">")) {
					String[] l1 = lines.get(l).split(":");
					if (l1.length != 2) {
						throw new RuntimeException("bad line: " + lines.get(l));
					}

					String nameVariantType = l1[0];
					if ("DELINS".equals(l1[0]) || "DEL".equals(l1[0])) {
						nameVariantType = "deletion";
					} else if ("INS".equals(l1[0])) {
						nameVariantType = "insertion";
					}
					variantType = VariantType.findByName(nameVariantType);
					ref = l1[1].split(">")[0];
					alts = l1[1].split(">")[1].split(",");
					l++;
				} else {
					variantType = VariantType.SNV;
					ref = "A";
					alts = new String[]{ "G" };
				}

				String[] l2 = lines.get(l).split(":");
				if (l2.length != 2) {
					throw new RuntimeException("bad line: " + lines.get(l));
				}
				l++;

				Chromosome chromosome = Chromosome.of(l2[0]);
				int start = Integer.parseInt(l2[1]);
				int end;
				if (variantType == VariantType.SNV) {
					end = start;
				} else if (variantType == VariantType.DEL) {
					end = start + ref.length() - 1;
				} else if (variantType == VariantType.INS) {
					end = start - 1;
				} else {
					throw new RuntimeException();
				}

//				if (!"rs1042485".equals(l0)) continue;

				for (String alt : alts) {
					GetAnfisaJSONController.RequestItem requestItem = new GetAnfisaJSONController.RequestItem(
							chromosome,
							start,
							end,
							alt
					);

					requestItems.add(requestItem);
				}
				requestIds.add(l0);
			}

//			ensemblVepService.getVepJson(requestIds.get(0))
//					.thenApply(vepJson -> {
//						log.debug("id: {}", vepJson);
//						return null;
//					});
//			GetAnfisaJSONController.RequestItem requestItem1 = requestItems.get(0);
//			ensemblVepService.getVepJson(requestItem1.chromosome, requestItem1.start, requestItem1.end, requestItem1.alternative)
//					.thenApply(vepJson -> {
//						log.debug("region: {}", vepJson);
//						return null;
//					});


			LinkedHashMap<String, Sample> samples = new LinkedHashMap<>();
//			samples.put("KCNJ2", new Sample(
//					"KCNJ2", "KCNJ12", "", "0", "0", 0, true, null
//			));
			MCase mCase = new MCase.Builder(samples, Collections.emptyList()).build();

			List<CompletableFuture<List<ProcessingResult>>> futureProcessingResults = new ArrayList<>();
			for (String id : requestIds) {
				futureProcessingResults.add(
						ensemblVepService.getVepJson(id)
								.thenApply(vepJson -> {
									try {
										Chromosome chromosome = Chromosome.of(vepJson.getAsString("seq_region_name"));
										int start = Integer.parseInt(vepJson.getAsString("start"));
										int end = Integer.parseInt(vepJson.getAsString("end"));

										VariantVep variantVep = new VariantCustom(chromosome, start, end);
										variantVep.setVepJson(vepJson);
										return processing.exec(mCase, variantVep);
									} catch (Throwable e) {
										log.error("vepjson: " + vepJson, e);
										throw e;
									}
								})
				);
			}
			/*
			for (GetAnfisaJSONController.RequestItem requestItem : requestItems) {
				futureProcessingResults.add(
						ensemblVepService.getVepJson(requestItem.chromosome, requestItem.start, requestItem.end, requestItem.alternative)
								.thenApply(vepJson -> {
									VariantVep variantVep = new VariantCustom(requestItem.chromosome, requestItem.start, requestItem.end);
									variantVep.setVepJson(vepJson);
									return processing.exec(mCase, variantVep);
								})
				);
			}
			*/


			OutputStream os = new GZIPOutputStream(Files.newOutputStream(Paths.get("KCNJ2_anfisa.json.gz")));
			BufferedOutputStream bos = new BufferedOutputStream(os);

			AnnotatorResult.Metadata metadata = AnnotatorResult.Metadata.build(
					"KCNJ2", null, mCase, processing.getAnfisaConnector()
			);
			bos.write(metadata.toJSON().toJSONString().getBytes(StandardCharsets.UTF_8));
			bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

			try {
				for (int i = 0; i < futureProcessingResults.size(); i++) {
					List<ProcessingResult> processingResults;
					try {
						processingResults = futureProcessingResults.get(i).join();
						for (ProcessingResult processingResult : processingResults) {

							String out = processingResult.toJSON().toJSONString();
							bos.write(out.getBytes(StandardCharsets.UTF_8));
							bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

							log.debug("Processing {}/{}", i + 1, futureProcessingResults.size());
						}
					} catch (Throwable e) {
						log.error("Exception ", e);
					}
				}
				bos.flush();
				bos.close();
				os.close();

				System.exit(0);
			} catch (Throwable ex) {
				fail(ex);
			}


		} catch (Throwable ex) {
			log.error("Exception: ", ex);
			new HelpFormatter().printHelp("", options);

			fail(ex);
		}
	}

	private static void fail(Throwable e) {
		org.forome.annotation.Main.crash(e);
	}

}
