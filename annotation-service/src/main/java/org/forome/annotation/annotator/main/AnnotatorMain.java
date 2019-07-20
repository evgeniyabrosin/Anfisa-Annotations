package org.forome.annotation.annotator.main;

import org.forome.annotation.annotator.main.argument.Arguments;
import org.forome.annotation.annotator.main.argument.ArgumentsAnnotation;
import org.forome.annotation.annotator.main.argument.ArgumentsVersion;
import org.forome.annotation.annotator.main.argument.ParserArgument;
import org.forome.annotation.utils.AppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            AnnotationConsole annotationConsole = new AnnotationConsole((ArgumentsAnnotation) arguments);
            annotationConsole.execute();
        }
    }

}
