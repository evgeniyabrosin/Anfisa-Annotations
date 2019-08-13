package org.forome.annotation.annotator.main;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.forome.annotation.Main;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.GnomadConnectorImpl;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
import org.forome.annotation.struct.CasePlatform;
import org.forome.annotation.utils.AppVersion;
import org.forome.annotation.utils.RuntimeExec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

public class AnnotationConsole {

    private final static Logger log = LoggerFactory.getLogger(AnnotationConsole.class);

    private final String caseName;
    private final CasePlatform casePlatform;

    private final Path famFile;
    private final Path patientIdsFile;

    private final Path vcfFile;
    private final Path vepJsonFile;

    private final int startPosition;

    private final Path outFile;

    private final Supplier<String> arguments;

    private final Instant timeStart;

    private ServiceConfig serviceConfig;
    private NotificationService notificationService;
    private SSHConnectService sshTunnelService;

    private GnomadConnector gnomadConnector;
    private SpliceAIConnector spliceAIConnector;
    private ConservationConnector conservationConnector;
    private HgmdConnector hgmdConnector;
    private ClinvarConnector clinvarConnector;
    private LiftoverConnector liftoverConnector;
    private GTFConnector gtfConnector;
    private AnfisaConnector anfisaConnector;

    public AnnotationConsole(
            Path configFile,
            String caseName, CasePlatform casePlatform,
            Path famFile, Path patientIdsFile,
            Path vcfFile, Path vepJsonFile,
            int startPosition,
            Path outFile,
            Supplier<String> arguments
    ) {
        this.caseName = caseName;
        this.casePlatform = casePlatform;

        this.famFile = famFile;
        this.patientIdsFile = patientIdsFile;

        this.vcfFile = vcfFile;
        this.vepJsonFile = vepJsonFile;

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
//            gnomadConnector = new GnomadConnectorOld(sshTunnelService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, arguments));
            gnomadConnector = new GnomadConnectorImpl(sshTunnelService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e, arguments));
            spliceAIConnector = new SpliceAIConnector(sshTunnelService, serviceConfig.spliceAIConfigConnector, (t, e) -> fail(e, arguments));
            conservationConnector = new ConservationConnector(sshTunnelService, serviceConfig.conservationConfigConnector);
            hgmdConnector = new HgmdConnector(sshTunnelService, serviceConfig.hgmdConfigConnector);
            clinvarConnector = new ClinvarConnector(sshTunnelService, serviceConfig.clinVarConfigConnector);
            liftoverConnector = new LiftoverConnector();
            gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, (t, e) -> fail(e, arguments));
            anfisaConnector = new AnfisaConnector(
                    gnomadConnector,
                    spliceAIConnector,
                    conservationConnector,
                    hgmdConnector,
                    clinvarConnector,
                    liftoverConnector,
                    gtfConnector,
                    (t, e) -> fail(e, arguments)
            );
        } catch (Throwable e) {
            fail(e, arguments);
        }
    }

    public void execute() {
        try {
            log.info("Input caseName: {}", caseName);
            log.info("Input famFile: {}", famFile);
            log.info("Input vepVcfFile: {}", vcfFile);
            log.info("Input start position: {}", startPosition);
            log.info("Input vepJsonFile: {}", (vepJsonFile != null) ? vepJsonFile : null);

            Path pathVepJson;
            if (vepJsonFile != null) {
                pathVepJson = vepJsonFile;
            } else {
                Path pathDirVepJson = outFile.getParent();

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
                pathVepJson = pathDirVepJson.resolve(fileNameVepJson).toAbsolutePath();

                String cmd = new StringBuilder("/db/vep-93/ensembl-vep/vep ")
                        .append("--buffer_size 50000 ")
                        .append("--cache --dir /db/data/vep/cache --dir_cache /db/data/vep/cache ")
                        .append("--fork 8 ")
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
                int codeRun;
                try {
                    codeRun = RuntimeExec.runCommand(cmd);
                } catch (Exception e) {
                    throw new RuntimeException("Exception run ensembl-vep", e);
                }
                if (codeRun != 0) {
                    throw new RuntimeException("Exception run ensembl-vep, return code: " + codeRun);
                }
                long t2 = System.currentTimeMillis();
                long sizeVepJson;
                try {
                    sizeVepJson = Files.size(pathVepJson);
                } catch (IOException e) {
                    throw new RuntimeException("Exception", e);
                }
                log.info("Run external ensembl-vep complete, time: {}, size vep.json: {}", t2 - t1, sizeVepJson);
            }


            Annotator annotator = new Annotator(anfisaConnector);
            AnnotatorResult annotatorResult = annotator.exec(
                    caseName,
                    casePlatform,
                    famFile,
                    patientIdsFile,
                    vcfFile,
                    pathVepJson,
                    startPosition
            );
            Files.deleteIfExists(outFile);
            Files.createFile(outFile);
            AtomicInteger count = new AtomicInteger();

            OutputStream os = buildOutputStream(outFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);

            String outMetadata = GetAnfisaJSONController.build(annotatorResult.metadata).toJSONString();
            bos.write(outMetadata.getBytes(StandardCharsets.UTF_8));
            bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

            annotatorResult.observableAnfisaResult.blockingSubscribe(
                    anfisaResult -> {
                        String out = GetAnfisaJSONController.build(anfisaResult).toJSONString();
                        bos.write(out.getBytes(StandardCharsets.UTF_8));
                        bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

                        if (count.getAndIncrement() % 100 == 0) {
                            log.debug("progress (count): {}", count.get());
                        }
                    },
                    e -> fail(e, arguments),
                    () -> {
                        log.debug("progress completed");
                        bos.close();
                        os.close();
                        anfisaConnector.close();
                        sendNotification(null, arguments);
                        System.exit(0);
                    }
            );
        } catch (Throwable e) {
            fail(e, arguments);
        }
    }

    private void fail(Throwable e, Supplier<String> arguments) {
        try {
            Files.deleteIfExists(outFile);
        } catch (Throwable e1) {
            log.error("Exception clear file: " + outFile, e);
        }
        sendNotification(e, arguments);
        Main.crash(e);
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

}
