package org.forome.annotation.annotator.main;

import io.reactivex.Observable;
import org.forome.annotation.Main;
import org.forome.annotation.annotator.Annotator;
import org.forome.annotation.config.ServiceConfig;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.controller.GetAnfisaJSONController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
            ServiceConfig serviceConfig = new ServiceConfig();
            GnomadConnector gnomadConnector = new GnomadConnector(serviceConfig.gnomadConfigConnector, (t, e) -> {
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
                    hgmdConnector,
                    clinvarConnector,
                    liftoverConnector,
                    gtfConnector
            );

            Annotator annotator = new Annotator(anfisaConnector);
            Observable<AnfisaResult> result = annotator.exec(
                    arguments.caseName,
                    arguments.pathFam,
                    arguments.pathVepFilteredVcf,
                    arguments.pathVepFilteredVepJson,
                    0
            );
            Files.deleteIfExists(arguments.pathOutput);
            Files.createFile(arguments.pathOutput);
            try (OutputStream os = Files.newOutputStream(arguments.pathOutput)) {
                try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                    result.blockingSubscribe(
                            anfisaResult -> {
                                String out = GetAnfisaJSONController.build(anfisaResult).toJSONString();
                                bos.write(out.getBytes(StandardCharsets.UTF_8));
                                bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                            },
                            e -> {
                                Main.crash(e);
                            }
                    );
                }
            }
        } catch (Throwable e) {
            log.error("Exception arguments parser", e);
            System.exit(2);
            return;
        }
    }


}
