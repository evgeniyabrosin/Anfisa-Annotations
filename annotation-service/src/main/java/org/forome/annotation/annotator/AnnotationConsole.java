/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.annotator;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.forome.annotation.Main;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.mysql.ClinvarConnectorMysql;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.gnomad.GnomadConnectorImpl;
import org.forome.annotation.data.gnomad.datasource.http.GnomadDataSourceHttp;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.mysql.GTEXConnectorMysql;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.http.GTFConnectorHttp;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.hgmd.mysql.HgmdConnectorMysql;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.pharmgkb.mysql.PharmGKBConnectorMysql;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnectorImpl;
import org.forome.annotation.data.spliceai.datasource.http.SpliceAIDataSourceHttp;
import org.forome.annotation.processing.Processing;
import org.forome.annotation.processing.TypeQuery;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.service.ensemblvep.EnsemblVepService;
import org.forome.annotation.service.ensemblvep.external.EnsemblVepExternalService;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.CasePlatform;
import org.forome.annotation.utils.AppVersion;
import org.forome.annotation.utils.RuntimeExec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AnnotationConsole {

	private final static Logger log = LoggerFactory.getLogger(AnnotationConsole.class);

	private final String caseName;

	private final Assembly assembly;
	private final CasePlatform casePlatform;

	private final Path famFile;
	private final Path patientIdsFile;

	private final Path pathCohorts;

	private final Path inputVcfFile;
	private final Path inputVepJsonFile;

	private final Path cnvFile;

	private final int startPosition;

	private final Path outFile;

	private final Supplier<String> arguments;

	private final Instant timeStart;

	private ServiceConfig serviceConfig;
	private NotificationService notificationService;
	private SSHConnectService sshTunnelService;
	private DatabaseConnectService databaseConnectService;

	private GnomadConnectorImpl gnomadConnector;
	private SpliceAIConnector spliceAIConnector;
	private ConservationData conservationConnector;
	private HgmdConnector hgmdConnector;
	private ClinvarConnector clinvarConnector;
	private LiftoverConnector liftoverConnector;
	private GTFConnector gtfConnector;
	private GTEXConnector gtexConnector;
	private PharmGKBConnector pharmGKBConnector;
	private EnsemblVepService ensemblVepService;
	private AnfisaConnector anfisaConnector;
	private Processing processing;

	public AnnotationConsole(
			Path configFile,
			String caseName,
			Assembly assembly, CasePlatform casePlatform,
			Path famFile, Path patientIdsFile,
			Path pathCohorts,
			Path vcfFile, Path vepJsonFile,
			Path cnvFile,
			int startPosition,
			Path outFile,
			Supplier<String> arguments
	) {
		this.caseName = caseName;

		this.assembly = assembly;
		this.casePlatform = casePlatform;

		this.famFile = famFile;
		this.patientIdsFile = patientIdsFile;

		this.pathCohorts = pathCohorts;

		this.inputVcfFile = vcfFile;
		this.inputVepJsonFile = vepJsonFile;

		this.cnvFile = cnvFile;

		this.startPosition = startPosition;

		this.outFile = outFile;

		this.arguments = arguments;

		this.timeStart = Instant.now();

		try {
			serviceConfig = new ServiceConfig(configFile);

			if (serviceConfig.notificationSlackConfig != null) {
				notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
			}

			sshTunnelService = new SSHConnectService();
			databaseConnectService = new DatabaseConnectService(sshTunnelService, serviceConfig.databaseConfig);
//            gnomadConnector = new GnomadConnectorOld(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, arguments));

			liftoverConnector = new LiftoverConnector();

			gnomadConnector = new GnomadConnectorImpl(
					new GnomadDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector),
					(t, e) -> fail(e, null, arguments)
			);
//			gnomadConnector = new GnomadConnectorImpl(databaseConnectService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, null, arguments));

			spliceAIConnector = new SpliceAIConnectorImpl(
					new SpliceAIDataSourceHttp(databaseConnectService, liftoverConnector, serviceConfig.aStorageConfigConnector)
			);
//			spliceAIConnector = new SpliceAIConnector(databaseConnectService, serviceConfig.spliceAIConfigConnector);

			conservationConnector = new ConservationData(databaseConnectService);

//			this.hgmdConnector = new HgmdConnectorHttp();
			this.hgmdConnector = new HgmdConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.hgmdConfigConnector);

//			clinvarConnector = new ClinvarConnectorHttp();
			clinvarConnector = new ClinvarConnectorMysql(databaseConnectService, liftoverConnector, serviceConfig.foromeConfigConnector);

			gtfConnector = new GTFConnectorHttp();
//			gtfConnector = new GTFConnectorMysql(databaseConnectService, serviceConfig.gtfConfigConnector, (t, e) -> fail(e, null, arguments));

//			gtexConnector = new GTEXConnectorHttp();
			gtexConnector = new GTEXConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

//			pharmGKBConnector = new PharmGKBConnectorHttp();
			pharmGKBConnector = new PharmGKBConnectorMysql(databaseConnectService, serviceConfig.foromeConfigConnector);

			ensemblVepService = new EnsemblVepExternalService((t, e) -> fail(e, null, arguments));
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
			processing = new Processing(anfisaConnector, TypeQuery.PATIENT_HG19);
		} catch (Throwable e) {
			fail(e, null, arguments);
		}
	}

	public void execute() {
		Path vcfFile = null;
		try {
			log.info("Version: {}", AppVersion.getVersion());
			log.info("Input caseName: {}", caseName);
			log.info("Input famFile: {}", famFile);
			log.info("Input cohortFile: {}", pathCohorts);
			log.info("Input vepVcfFile: {}", inputVcfFile);
			log.info("Input start position: {}", startPosition);
			log.info("Input vepJsonFile: {}", inputVepJsonFile);
			log.info("Input cnvFile: {}", cnvFile);

			if (!inputVcfFile.getFileName().toString().endsWith(".gz")) {
				vcfFile = inputVcfFile;
			} else {
				Path pathDir = outFile.getParent();
				log.info("unpacking vcf file: {}...", inputVcfFile);
				vcfFile = gunzipVcfFile(inputVcfFile, pathDir);
				log.info("unpacking vcf file: {}... complete", inputVcfFile);
			}
			Path finalVcfFile = vcfFile;

			//Билдим при необходимости vep-json
			Path vepJson;
			if (inputVepJsonFile != null) {
				vepJson = inputVepJsonFile;
			} else {
				Path pathDirVepJson = outFile.getParent();
				vepJson = buildVepJson(vcfFile, pathDirVepJson);
			}

			Annotator annotator = new Annotator(ensemblVepService, processing);
			AnnotatorResult annotatorResult = annotator.exec(
					caseName,
					assembly,
					casePlatform,
					famFile,
					patientIdsFile,
					pathCohorts,
					vcfFile,
					vepJson,
					cnvFile,
					startPosition
			);
			Files.deleteIfExists(outFile);
			Files.createFile(outFile);
			AtomicInteger count = new AtomicInteger();

			OutputStream os = buildOutputStream(outFile);
			BufferedOutputStream bos = new BufferedOutputStream(os);

			String outMetadata = annotatorResult.metadata.toJSON().toJSONString();
			bos.write(outMetadata.getBytes(StandardCharsets.UTF_8));
			bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

			annotatorResult.observableAnfisaResult.blockingSubscribe(
					processingResult -> {
						String out = processingResult.toJSON().toJSONString();
						bos.write(out.getBytes(StandardCharsets.UTF_8));
						bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

						if (count.getAndIncrement() % 100 == 0) {
							log.debug("progress (count): {}", count.get());
						}
					},
					e -> fail(e, finalVcfFile, arguments),
					() -> {
						log.debug("progress completed");
						log.debug("conservation statistics: {}", conservationConnector.getStatistics());
						bos.close();
						os.close();
						anfisaConnector.close();
						clear(finalVcfFile);
						sendNotification(null, arguments);
						System.exit(0);
					}
			);
		} catch (Throwable e) {
			fail(e, vcfFile, arguments);
		}
	}

	private void fail(Throwable e,  Path vcfFile, Supplier<String> arguments) {
		if (Files.exists(outFile)) {
			String newFileName = new StringBuilder()
					.append(outFile.getFileName().toString())
					.append("_invalid_").append(timeStart.toEpochMilli())
					.toString();
			try {
				Files.move(outFile, outFile.getParent().resolve(newFileName));
			} catch (Throwable e1) {
				log.error("Exception clear file: " + outFile, e);
			}
		}
		clear(vcfFile);
		sendNotification(e, arguments);
		Main.crash(e);
	}

	private void clear(Path vcfFile) {
		try {
			if (vcfFile != null && !Files.isSameFile(inputVcfFile, vcfFile)) {
				log.info("clear tmp file: {}", vcfFile);
				Files.deleteIfExists(vcfFile);
			}
		} catch (IOException e) {
			log.error("Exception clears", e);
		}
	}

	private void sendNotification(Throwable throwable, Supplier<String> arguments) {
		try {
			StringBuilder messageBuilder = new StringBuilder();
			if (throwable == null) {
				messageBuilder.append("Success annotation case: ").append(caseName).append('\n');
				messageBuilder.append("Result: ").append(outFile).append('\n');
			} else {
				messageBuilder.append("FAIL!!! annotation case: ").append(caseName).append('\n');
			}
			messageBuilder.append('\n');
			messageBuilder.append("User: ").append(System.getProperty("user.name")).append('\n');
			messageBuilder.append("Version: ").append(AppVersion.getVersion()).append('\n');

			messageBuilder.append('\n');
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss Z")
					.withZone(ZoneId.systemDefault());
			messageBuilder.append("Time start: ").append(formatter.format(timeStart)).append('\n');
			messageBuilder.append("Time complete: ").append(formatter.format(Instant.now())).append('\n');

			messageBuilder.append('\n');
			messageBuilder.append("Run arguments:").append('\n');
			messageBuilder.append(arguments.get()).append('\n');

			if (throwable != null) {
				messageBuilder.append('\n');
				messageBuilder.append("Exception: ").append(ExceptionUtils.getStackTrace(throwable)).append('\n');
			}

			notificationService.send(messageBuilder.toString());
		} catch (Throwable e) {
			log.error("Exception send notification", e);
		}
	}

	private static OutputStream buildOutputStream(Path pathOutput) throws IOException {
		if (pathOutput.getFileName().toString().endsWith(".gz")) {
			return new GZIPOutputStream(Files.newOutputStream(pathOutput));
		} else {
			return Files.newOutputStream(pathOutput);
		}
	}

	private static Path gunzipVcfFile(Path vcfFile, Path pathDir) throws IOException {
		if (!vcfFile.getFileName().toString().endsWith(".vcf.gz")) {
			throw new RuntimeException("VcfFile is not *.vcf.gz" + vcfFile.toAbsolutePath());
		}

		String fileNameVcfFile = vcfFile.getFileName().toString();
		String s = fileNameVcfFile.substring(0, fileNameVcfFile.length() - ".vcf.gz".length());
		fileNameVcfFile = s + ".vcf";
		int i = 0;
		while (Files.exists(pathDir.resolve(fileNameVcfFile))) {
			fileNameVcfFile = String.format("%s(%s).vcf", s, ++i);
		}
		Path pathVcfFile = pathDir.resolve(fileNameVcfFile).toAbsolutePath();

		try (FileInputStream fis = new FileInputStream(vcfFile.toFile())) {
			try(GZIPInputStream gis = new GZIPInputStream(fis)) {
				try(FileOutputStream fos = new FileOutputStream(pathVcfFile.toFile())) {
					byte[] buffer = new byte[1024];
					int len;
					while((len = gis.read(buffer)) != -1){
						fos.write(buffer, 0, len);
					}
				} catch (Throwable e) {
					Files.deleteIfExists(pathVcfFile);
					throw e;
				}
			}
		}
		return pathVcfFile;
	}

	private static Path buildVepJson(Path vcfFile, Path pathDirVepJson) {
		String fileNameVcf = vcfFile.getFileName().toString();
		String fileNameVepJson;
		if (fileNameVcf.endsWith(".vcf")) {
			String s = fileNameVcf.substring(0, fileNameVcf.length() - ".vcf".length());
			fileNameVepJson = s + ".vep.json";
			int i = 0;
			while (Files.exists(pathDirVepJson.resolve(fileNameVepJson))) {
				fileNameVepJson = String.format("%s(%s).vep.json", s, ++i);
			}
		} else {
			throw new IllegalArgumentException("Bad vcf filename (Need *.vcf): " + vcfFile.toAbsolutePath());
		}
		Path pathVepJson = pathDirVepJson.resolve(fileNameVepJson).toAbsolutePath();

		String cmd = new StringBuilder("/db/vep-93/ensembl-vep/vep ")
				.append("--buffer_size 50000 ")
				.append("--cache --dir /db/data/vep/cache --dir_cache /db/data/vep/cache ")
				.append("--fork 4 ")
				.append("--uniprot --hgvs --symbol --numbers --domains --regulatory --canonical --protein --biotype --tsl --appris --gene_phenotype --variant_class ")
				.append("--fasta /db/data/vep/cache/homo_sapiens/93_GRCh37/Homo_sapiens.GRCh37.75.dna.primary_assembly.fa.gz ")
				.append("--force_overwrite ")
				.append("--merged ")
				.append("--json ")
				.append("--port 3337 ")
				.append("--input_file ").append(vcfFile).append(' ')
				.append("--output_file ").append(pathVepJson.toAbsolutePath()).append(' ')
				.append("--plugin ExACpLI,/db/data/misc/ExACpLI_values.txt ")
				.append("--plugin MaxEntScan,/db/data/MaxEntScan/fordownload ")
				.append("--plugin LoFtool,/db/data/loftoll/LoFtool_scores.txt ")
				.append("--plugin dbNSFP,/db/data/dbNSFPa/dbNSFP_hg19.gz,Polyphen2_HDIV_pred,Polyphen2_HVAR_pred,Polyphen2_HDIV_score,Polyphen2_HVAR_score,SIFT_pred,SIFT_score,MutationTaster_pred,MutationTaster_score,FATHMM_pred,FATHMM_score,REVEL_score,CADD_phred,CADD_raw,MutationAssessor_score,MutationAssessor_pred,clinvar_rs,clinvar_clnsig ")
				.append("--plugin SpliceRegion ")
				.append("--everything")
				.toString();

		log.info("run external ensembl-vep, cmd: {}", cmd);
		long t1 = System.currentTimeMillis();

		RuntimeExec.Result result;
		try {
			result = RuntimeExec.runCommand(cmd);
		} catch (Exception e) {
			throw new RuntimeException("Exception run ensembl-vep", e);
		}
		if (result.exitCode != 0) {
			throw new RuntimeException("Exception run ensembl-vep, return code: '" + result.exitCode + "' out: '" + result.out + "', error out: " + result.outError);
		}
		long t2 = System.currentTimeMillis();
		long sizeVepJson;
		try {
			sizeVepJson = Files.size(pathVepJson);
		} catch (IOException e) {
			throw new RuntimeException("Exception", e);
		}
		log.info("Run external ensembl-vep complete, time: {}, size vep.json: {}", t2 - t1, sizeVepJson);

		return pathVepJson;
	}

}
