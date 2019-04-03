package org.forome.annotation.annotator.main;

import org.apache.commons.cli.*;
import org.forome.annotation.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AnnotatorArgumentParser {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    private static final String OPTION_CASE_NAME = "case";
    private static final String OPTION_FILE_FAM = "fam";
    private static final String OPTION_FILE_VCF = "vcf";
    private static final String OPTION_FILE_VEP_JSON = "vepjson";
    private static final String OPTION_FILE_OUTPUT = "output";

    public final String caseName;
    public final Path pathFam;
    public final Path pathVepFilteredVcf;
    public final Path pathVepFilteredVepJson;
    public final Path pathOutput;

    public AnnotatorArgumentParser(String[] args) throws InterruptedException {
        Options options = new Options()
                .addOption(Option.builder()
                        .longOpt(OPTION_CASE_NAME)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Case name")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_FAM)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to fam file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_VCF)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to vcf file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_VEP_JSON)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to vep.json file")
                        .build())
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_OUTPUT)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to output file")
                        .build());

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            this.caseName = cmd.getOptionValue(OPTION_CASE_NAME);
            this.pathFam = Paths.get(cmd.getOptionValue(OPTION_FILE_FAM));
            this.pathVepFilteredVcf = Paths.get(cmd.getOptionValue(OPTION_FILE_VCF));
            this.pathVepFilteredVepJson = Paths.get(cmd.getOptionValue(OPTION_FILE_VEP_JSON));
            this.pathOutput = Paths.get(cmd.getOptionValue(OPTION_FILE_OUTPUT));

        } catch (Throwable ex) {
            System.out.println(ex.getMessage());
            new HelpFormatter().printHelp("", options);

            throw new InterruptedException();
        }
    }
}
