package org.forome.annotation.annotator.main;

import org.forome.annotation.Main;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.main.argument.Arguments;
import org.forome.annotation.annotator.main.argument.ArgumentsAnnotation;
import org.forome.annotation.annotator.main.argument.ArgumentsVersion;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * cd /data/bgm/cases/bgm9001/
 * java -cp /home/vulitin/deploy/annotationservice/exec/annotation.jar org.forome.annotation.annotator.main.AnnotatorMain -config /home/vulitin/deploy/annotationservice/exec/config.json -vcf bgm9001_wgs_xbrowse.vep.vcf -vepjson bgm9001_wgs_xbrowse.vep.vep.json -output bgm9001_wgs_xbrowse.out.json
 * Для 6 милионов 37:09:11.460
 */
public class AnnotatorMain {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorMain.class);

    public static void main(String[] args) {
        Arguments arguments;
        try {
            ParserArgument argumentParser = new ParserArgument(args);
            arguments = argumentParser.arguments;
        } catch (Throwable e) {
            log.error("Exception arguments parser", e);
            System.exit(2);
            return;
        }

        if (arguments instanceof ArgumentsVersion) {
            System.out.println("Version: " + AppVersion.getVersion());
            System.out.println("Version Format: " + AppVersion.getVersionFormat());
        } else if (arguments instanceof ArgumentsAnnotation) {
            annotation((ArgumentsAnnotation) arguments);
        }
    }

    private static void annotation(ArgumentsAnnotation arguments) {
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
            log.info("Run external ensembl-vep complete, time: {}, size vep.json: {}", t2-t1, sizeVepJson);
        }

        try {
            ServiceConfig serviceConfig = new ServiceConfig(arguments.config);
            GnomadConnector gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
                fail(e, arguments.pathOutput);
            });
            SpliceAIConnector spliceAIConnector = new SpliceAIConnector(serviceConfig.spliceAIConfigConnector, (t, e) -> {
                fail(e, arguments.pathOutput);
            });
            ConservationConnector conservationConnector = new ConservationConnector(serviceConfig.conservationConfigConnector);
            HgmdConnector hgmdConnector = new HgmdConnector(serviceConfig.hgmdConfigConnector);
            ClinvarConnector clinvarConnector = new ClinvarConnector(serviceConfig.clinVarConfigConnector);
            LiftoverConnector liftoverConnector = new LiftoverConnector();
            GTFConnector gtfConnector = new GTFConnector(serviceConfig.gtfConfigConnector, (t, e) -> {
                fail(e, arguments.pathOutput);
            });
            AnfisaConnector anfisaConnector = new AnfisaConnector(
                    gnomadConnector,
                    spliceAIConnector,
                    conservationConnector,
                    hgmdConnector,
                    clinvarConnector,
                    liftoverConnector,
                    gtfConnector,
                    (t, e) -> {
                        fail(e, arguments.pathOutput);
                    }
            );

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
                    e -> {
                        fail(e, arguments.pathOutput);
                    },
                    () -> {
                        log.debug("progress completed");
                        bos.close();
                        os.close();
                        anfisaConnector.close();
                        System.exit(0);
                    }
            );
        } catch (Throwable e) {
            log.error("Exception arguments parser", e);
            System.exit(2);
            return;
        }
    }

    private static void fail(Throwable e, Path pathOutput) {
        try {
            Files.deleteIfExists(pathOutput);
        } catch (Throwable e1) {
            log.error("Exception clear file: " + pathOutput, e);
        }
        Main.crash(e);
    }

    private static OutputStream buildOutputStream(Path pathOutput) throws IOException {
        if (pathOutput.getFileName().toString().endsWith(".gz")) {
            return new GZIPOutputStream(Files.newOutputStream(pathOutput));
        } else {
            return Files.newOutputStream(pathOutput);
        }
    }

}
