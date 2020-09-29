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
import net.minidev.json.JSONObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.forome.annotation.annotator.struct.AnnotatorResultMetadata;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.astorage.AStorageHttp;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.mysql.ClinvarConnectorMysql;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.fasta.FastaSource;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gnomad.datasource.http.GnomadDataSourceHttp;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.mysql.GTEXConnectorMysql;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.GTFConnectorImpl;
import org.forome.annotation.data.gtf.datasource.http.GTFDataSourceHttp;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.mysql.PharmGKBConnectorMysql;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnectorImpl;
import org.forome.annotation.data.spliceai.datasource.http.SpliceAIDataSourceHttp;
import org.forome.annotation.iterator.vepjson.VepJsonFileIterator;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
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
import java.util.zip.GZIPOutputStream;

public class CustomInputMain {

	private final static Logger log = LoggerFactory.getLogger(CustomInputMain.class);

	public static final String OPTION_FILE_CONFIG = "config";

	public static final String OPTION_FILE_INPUT = "input";
	public static final String OPTION_FILE_VEPJSON = "vepjson";

	private static ServiceConfig serviceConfig;
	private static NotificationService notificationService;
	private static SSHConnectService sshTunnelService;
	private static DatabaseConnectService databaseConnectService;

	private static GnomadConnectorImpl gnomadConnector;
	private static SpliceAIConnector spliceAIConnector;
	private static ConservationData conservationConnector;
	private static HgmdConnector hgmdConnector;
	private static ClinvarConnector clinvarConnector;
	private static LiftoverConnector liftoverConnector;
	private static FastaSource fastaSource;
	private static GTFConnector gtfConnector;
	private static GTEXConnector gtexConnector;
	private static PharmGKBConnector pharmGKBConnector;
	private static AStorageHttp sourceHttp38;
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
						.build())

				.addOption(Option.builder()
						.longOpt(OPTION_FILE_VEPJSON)
						.hasArg(true)
						.optionalArg(false)
						.desc("Absolute path to file vep.json")
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

			String strFileVepJson = cmd.getOptionValue(OPTION_FILE_VEPJSON);
			if (Strings.isNullOrEmpty(strFileVepJson)) {
				throw new IllegalArgumentException("Missing vep.json file");
			}
			Path pathFileVepJson = Paths.get(strFileVepJson).toAbsolutePath();
			if (!Files.exists(pathInputFile)) {
				throw new IllegalArgumentException("Vep.json file is not exists: " + config);
			}

			serviceConfig = new ServiceConfig(config);

			if (serviceConfig.notificationSlackConfig != null) {
				notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
			}

			sshTunnelService = new SSHConnectService();
			databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
//            gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, arguments));

			liftoverConnector = new LiftoverConnector();
			fastaSource = new FastaSource(databaseConnectService, serviceConfig.aStorageConfigConnector);

			gnomadConnector = new GnomadConnectorImpl(new GnomadDataSourceHttp(databaseConnectService, liftoverConnector, fastaSource, serviceConfig.aStorageConfigConnector),
					(t, e) -> fail(e)
			);
//			gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e));

			spliceAIConnector = new SpliceAIConnectorImpl(
					new SpliceAIDataSourceHttp(liftoverConnector)
			);
//			spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

			conservationConnector = new ConservationData(databaseConnectService);

//			hgmdConnector = new HgmdConnectorHttp();
			hgmdConnector = new HgmdConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.hgmdConfigConnector);

//			clinvarConnector = new ClinvarConnectorHttp();
			clinvarConnector = new ClinvarConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.foromeConfigConnector);

			gtfConnector = new GTFConnectorImpl(
					new GTFDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
					liftoverConnector,
					(t, e) -> fail(e)
			);
			//gtfConnector = new GTFConnector(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> fail(e));

//			gtexConnector = new GTEXConnectorHttp();
			gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

//			pharmGKBConnector = new PharmGKBConnectorHttp();
			pharmGKBConnector = new PharmGKBConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

			sourceHttp38 = new AStorageHttp(
					databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector
			);

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
					pharmGKBConnector,
					sourceHttp38,
					fastaSource
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

			MCase mCase = new MCase.Builder(Assembly.GRCh37, new LinkedHashMap<>(), Collections.emptyList()).build();

//			Path pathIds = Paths.get("/home/kris/processtech/tmp/10/variants2_ids.txt");
//			try (OutputStream os = Files.newOutputStream(pathIds)){
//				try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
//					for (String id : requestIds) {
//						bos.write(id.getBytes(StandardCharsets.UTF_8));
//						bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
//					}
//				}
//			}

			try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(Paths.get("KCNJ2_anfisa.json.gz")))) {
				try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
					AnnotatorResultMetadata metadata = new AnnotatorResultMetadata(
							"KCNJ2", null, mCase, processing.getAnfisaConnector()
					);
					bos.write(metadata.toJSON().toJSONString().getBytes(StandardCharsets.UTF_8));
					bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

					int index = 0;
					try (VepJsonFileIterator jsonFileIterator = new VepJsonFileIterator(pathFileVepJson)) {
						while (jsonFileIterator.hasNext()) {
							JSONObject vepJson = jsonFileIterator.next();

							Chromosome chromosome = Chromosome.of(vepJson.getAsString("seq_region_name"));
							int start = Integer.parseInt(vepJson.getAsString("start"));
							int end = Integer.parseInt(vepJson.getAsString("end"));
							Allele alt = new Allele(vepJson.getAsString("allele_string").split("/")[1]);

							VariantVep variantVep = new VariantCustom(
									chromosome,
									start, end,
									null, //TODO Ulitin V. Не реализованно
									alt
							);
							variantVep.setVepJson(vepJson);

							ProcessingResult processingResult = processing.exec(mCase, variantVep);
//							for (ProcessingResult processingResult : processingResults) {
								String out = processingResult.toJSON().toJSONString();
								bos.write(out.getBytes(StandardCharsets.UTF_8));
								bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

								log.debug("Processing {}", index++);
//							}
						}
					}
				}
			}

			log.debug("Processing complete");
			System.exit(0);
		} catch (Throwable ex) {
			log.error("Exception: ", ex);
			fail(ex);
		}
	}

	private static void fail(Throwable e) {
		org.forome.annotation.Main.crash(e);
	}

}
