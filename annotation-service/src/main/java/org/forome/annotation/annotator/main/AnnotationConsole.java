package org.forome.annotation.annotator.main;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.forome.annotation.Main;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.main.argument.ArgumentsAnnotation;
import org.forome.annotation.annotator.main.argument.ParserArgument;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.conservation.ConservationConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.forome.annotation.service.notification.NotificationService;
import org.forome.annotation.service.ssh.SSHConnectService;
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
import java.util.zip.GZIPOutputStream;

public class AnnotationConsole {

    private final static Logger log = LoggerFactory.getLogger(AnnotationConsole.class);

    private final ArgumentsAnnotation arguments;
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

    public AnnotationConsole(ArgumentsAnnotation arguments) {
        this.arguments = arguments;
        this.timeStart = Instant.now();

        try {
            serviceConfig = new ServiceConfig(arguments.config);

            if (serviceConfig.notificationSlackConfig != null) {
                notificationService = new NotificationService(serviceConfig.notificationSlackConfig);
            }

            sshTunnelService = new SSHConnectService();
            gnomadConnector = new GnomadConnector(sshTunnelService, serviceConfig.gnomadConfigConnector, (t, e) -> fail(e));
            spliceAIConnector = new SpliceAIConnector(sshTunnelService, serviceConfig.spliceAIConfigConnector, (t, e) -> fail(e));
            conservationConnector = new ConservationConnector(sshTunnelService, serviceConfig.conservationConfigConnector);
            hgmdConnector = new HgmdConnector(sshTunnelService, serviceConfig.hgmdConfigConnector);
            clinvarConnector = new ClinvarConnector(sshTunnelService, serviceConfig.clinVarConfigConnector);
            liftoverConnector = new LiftoverConnector();
            gtfConnector = new GTFConnector(sshTunnelService, serviceConfig.gtfConfigConnector, (t, e) -> fail(e));
            anfisaConnector = new AnfisaConnector(
                    gnomadConnector,
                    spliceAIConnector,
                    conservationConnector,
                    hgmdConnector,
                    clinvarConnector,
                    liftoverConnector,
                    gtfConnector,
                    (t, e) -> fail(e)
            );
        } catch (Throwable e) {
            fail(e);
        }
    }

    public void execute() {
        try {
            log.info("Input caseName: {}", arguments.caseName);
            log.info("Input famFile: {}", arguments.pathFam.toAbsolutePath());
            log.info("Input vepVcfFile: {}", arguments.pathVcf.toAbsolutePath());
            log.info("Input start position: {}", arguments.start);
            log.info("Input vepJsonFile: {}", (arguments.pathVepJson != null) ? arguments.pathVepJson.toAbsolutePath() : null);

            Path pathVepJson;
            if (arguments.pathVepJson != null) {
                pathVepJson = arguments.pathVepJson.toAbsolutePath();
            } else {
                Path pathDirVepJson = arguments.pathOutput.toAbsolutePath().getParent();

                String fileNameVcf = arguments.pathVcf.getFileName().toString();
                String fileNameVepJson;
                if (fileNameVcf.endsWith(".vcf")) {
                    String s = fileNameVcf.substring(0, fileNameVcf.length() - ".vcf".length());
                    fileNameVepJson = s + ".vep.json";
                    int i = 0;
                    while (Files.exists(pathDirVepJson.resolve(fileNameVepJson))) {
                        fileNameVepJson = String.format("%s(%s).vep.json", s, ++i);
                    }
                } else {
                    throw new IllegalArgumentException("Bad vcf filename (Need *.vcf): " + arguments.pathVcf.toAbsolutePath());
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
                        .append("--input_file ").append(arguments.pathVcf.toAbsolutePath()).append(' ')
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
                    arguments.caseName,
                    arguments.pathFam,
                    arguments.pathFamSampleName,
                    arguments.pathVcf,
                    pathVepJson,
                    arguments.start
            );
            Files.deleteIfExists(arguments.pathOutput);
            Files.createFile(arguments.pathOutput);
            AtomicInteger count = new AtomicInteger();

            OutputStream os = buildOutputStream(arguments.pathOutput);
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
                    e -> fail(e),
                    () -> {
                        log.debug("progress completed");
                        bos.close();
                        os.close();
                        anfisaConnector.close();
                        sendNotification(null);
                        System.exit(0);
                    }
            );
        } catch (Throwable e) {
            fail(e);
        }
    }

    private void fail(Throwable e) {
        try {
            Files.deleteIfExists(arguments.pathOutput);
        } catch (Throwable e1) {
            log.error("Exception clear file: " + arguments.pathOutput, e);
        }
        sendNotification(e);
        Main.crash(e);
    }

    private void sendNotification(Throwable throwable) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            if (throwable == null) {
                messageBuilder.append("Success annotation case: ").append(arguments.caseName).append('\n');
                messageBuilder.append("Result: ").append(arguments.pathOutput).append('\n');
            } else {
                messageBuilder.append("FAIL!!! annotation case: ").append(arguments.caseName).append('\n');
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
            messageBuilder.append(" -").append(ParserArgument.OPTION_CASE_NAME).append(' ').append(arguments.caseName).append(" \\\n");
            messageBuilder.append(" -").append(ParserArgument.OPTION_FILE_FAM).append(' ').append(arguments.pathFam).append(" \\\n");
            if (arguments.pathFamSampleName != null) {
                messageBuilder.append(" -").append(ParserArgument.OPTION_FILE_FAM_NAME).append(' ').append(arguments.pathFamSampleName).append(" \\\n");
            }
            messageBuilder.append(" -").append(ParserArgument.OPTION_FILE_VCF).append(' ').append(arguments.pathVcf).append(" \\\n");
            if (arguments.pathVepJson != null) {
                messageBuilder.append(" -").append(ParserArgument.OPTION_FILE_VEP_JSON).append(' ').append(arguments.pathVepJson).append(" \\\n");
            }
            messageBuilder.append(" -").append(ParserArgument.OPTION_FILE_OUTPUT).append(' ').append(arguments.pathOutput).append('\n');

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
