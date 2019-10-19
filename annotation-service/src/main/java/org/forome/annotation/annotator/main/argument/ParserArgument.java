package org.forome.annotation.annotator.main.argument;

import org.apache.commons.cli.*;
import org.forome.annotation.annotator.main.AnnotatorMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserArgument {

    public static final String OPTION_VERSION = "version";

    public static final String OPTION_FILE_CONFIG = "config";

    public static final String OPTION_CASE_NAME = "name";
    public static final String OPTION_FILE_FAM = "fam";
    public static final String OPTION_FILE_FAM_NAME = "famname";
    public static final String OPTION_FILE_COHORT = "cohort";
    public static final String OPTION_FILE_VCF = "vcf";
    public static final String OPTION_FILE_VEP_JSON = "vepjson";
    public static final String OPTION_FILE_CNV = "cnv";
    public static final String OPTION_START_POSITION = "start";
    public static final String OPTION_FILE_OUTPUT = "output";

    public static final String OPTION_FILE_INVENTORY = "inventory";

    public final Arguments arguments;

    public ParserArgument(String[] args) throws InterruptedException {
        Options options = new Options()
                .addOption(Option.builder()
                        .longOpt(OPTION_VERSION)
                        .hasArg(false)
                        .optionalArg(false)
                        .desc("Version")
                        .build())

                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_CONFIG)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to config file")
                        .build())

                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_INVENTORY)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to inventory file")
                        .build())

                .addOption(Option.builder()
                        .longOpt(OPTION_CASE_NAME)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Case name")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_FAM)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Absolute path to *.fam file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_FAM_NAME)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Absolute path to fam names file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_COHORT)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Absolute path to cohort file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_VCF)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Absolute path to vcf file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_VEP_JSON)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Absolute path to vep.json file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_START_POSITION)
                        .hasArg(true)
                        .optionalArg(true)
                        .desc("Start position")
                        .type(Integer.class)
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_OUTPUT)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to output file")
                        .build());

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption(OPTION_VERSION)) {
                arguments = new ArgumentsVersion(cmd);
            } else if (cmd.hasOption(OPTION_FILE_INVENTORY)) {
                arguments = new ArgumentsInventory(cmd);
            } else {
                arguments = new ArgumentsAnnotation(cmd);
            }
        } catch (Throwable ex) {
            getLazyLogger().error("Exception: ", ex);
            new HelpFormatter().printHelp("", options);

            throw new InterruptedException();
        }
    }

    private static Logger getLazyLogger() {
        return LoggerFactory.getLogger(AnnotatorMain.class);
    }
}
