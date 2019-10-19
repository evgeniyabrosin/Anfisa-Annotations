package org.forome.annotation.annotator.main;

import org.forome.annotation.annotator.main.argument.*;
import org.forome.annotation.inventory.Inventory;
import org.forome.annotation.logback.LogbackConfigure;
import org.forome.annotation.utils.AppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cd /data/bgm/cases/bgm9001/
 * java -cp /home/vulitin/deploy/annotationservice/exec/annotation.jar org.forome.annotation.annotator.main.AnnotatorMain -config /home/vulitin/deploy/annotationservice/exec/config.json -vcf bgm9001_wgs_xbrowse.vep.vcf -vepjson bgm9001_wgs_xbrowse.vep.vep.json -output bgm9001_wgs_xbrowse.out.json
 * Для 6 милионов 37:09:11.460
 */
public class AnnotatorMain {

    public static void main(String[] args) {
        Arguments arguments;
        try {
            ParserArgument argumentParser = new ParserArgument(args);
            arguments = argumentParser.arguments;
        } catch (Throwable e) {
            getLazyLogger().error("Exception arguments parser", e);
            System.exit(2);
            return;
        }

        if (arguments instanceof ArgumentsVersion) {
            System.out.println("Version: " + AppVersion.getVersion());
            System.out.println("Version Format: " + AppVersion.getVersionFormat());
        } else if (arguments instanceof ArgumentsInventory) {
            ArgumentsInventory argumentsInventory = (ArgumentsInventory) arguments;
            Inventory inventory = new Inventory.Builder(argumentsInventory.pathInventory).build();
            if (inventory.logFile != null) {
                LogbackConfigure.setLogPath(inventory.logFile);
            }
            AnnotationConsole annotationConsole = new AnnotationConsole(
                    argumentsInventory.config,
                    inventory.caseName, inventory.casePlatform,
                    inventory.famFile, inventory.patientIdsFile,
                    inventory.cohortFile,
                    inventory.vcfFile, inventory.vepJsonFile,
                    inventory.cnvFile,
                    argumentsInventory.start,
                    inventory.outFile,
                    () -> arguments.getArguments()
            );
            annotationConsole.execute();
        } else if (arguments instanceof ArgumentsAnnotation) {
            ArgumentsAnnotation argumentsAnnotation = (ArgumentsAnnotation) arguments;
            AnnotationConsole annotationConsole = new AnnotationConsole(
                    argumentsAnnotation.config,
                    argumentsAnnotation.caseName, argumentsAnnotation.casePlatform,
                    argumentsAnnotation.pathFam, argumentsAnnotation.patientIdsFile,
                    argumentsAnnotation.cohortFile,
                    argumentsAnnotation.pathVcf, argumentsAnnotation.pathVepJson,
                    argumentsAnnotation.pathCnv,
                    argumentsAnnotation.start,
                    argumentsAnnotation.pathOutput,
                    () -> arguments.getArguments()
            );
            annotationConsole.execute();
        } else {
            getLazyLogger().error("Unknown arguments");
            System.exit(3);
            return;
        }
    }

    private static Logger getLazyLogger() {
        return LoggerFactory.getLogger(AnnotatorMain.class);
    }
}
