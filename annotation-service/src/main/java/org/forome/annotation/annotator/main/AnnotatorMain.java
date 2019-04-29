package org.forome.annotation.annotator.main;

import org.forome.annotation.Main;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.controller.GetAnfisaJSONController;
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

public class AnnotatorMain {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorMain.class);

    public static void main(String[] args) {
        AnnotatorArgumentParser arguments;
        try {
            arguments = new AnnotatorArgumentParser(args);
        } catch (Throwable e) {
            log.error("Exception arguments parser", e);
            System.exit(2);
            return;
        }

        try {
            ServiceConfig serviceConfig = new ServiceConfig(arguments.config);
            GnomadConnector gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
                Main.crash(e);
            });
            SpliceAIConnector spliceAIConnector = new SpliceAIConnector(serviceConfig.spliceAIConfigConnector, (t, e) -> {
                Main.crash(e);
            });
            HgmdConnector hgmdConnector = new HgmdConnector(serviceConfig.hgmdConfigConnector);
            ClinvarConnector clinvarConnector = new ClinvarConnector(serviceConfig.clinVarConfigConnector);
            LiftoverConnector liftoverConnector = new LiftoverConnector();
            GTFConnector gtfConnector = new GTFConnector(serviceConfig.gtfConfigConnector, (t, e) -> {
                Main.crash(e);
            });
            AnfisaConnector anfisaConnector = new AnfisaConnector(
                    gnomadConnector,
                    spliceAIConnector,
                    hgmdConnector,
                    clinvarConnector,
                    liftoverConnector,
                    gtfConnector
            );

            Annotator annotator = new Annotator(anfisaConnector);
            AnnotatorResult annotatorResult = annotator.exec(
                    arguments.caseName,
                    arguments.pathFam,
                    arguments.pathVepFilteredVcf,
                    arguments.pathVepFilteredVepJson,
                    0
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
                        Main.crash(e);
                    },
                    () -> {
                        bos.close();
                        os.close();
                        System.exit(0);
                    }
            );
        } catch (Throwable e) {
            log.error("Exception arguments parser", e);
            System.exit(2);
            return;
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
