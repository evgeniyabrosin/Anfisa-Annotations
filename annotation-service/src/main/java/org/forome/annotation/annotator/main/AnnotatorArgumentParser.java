package org.forome.annotation.annotator.main;

import org.apache.commons.cli.*;
import org.forome.annotation.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AnnotatorArgumentParser {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    private static final String OPTION_FILE_CONFIG = "config";
    private static final String OPTION_CASE_NAME = "name";
    private static final String OPTION_FILE_FAM = "fam";
    private static final String OPTION_FILE_FAM_NAME = "famname";
    private static final String OPTION_FILE_VCF = "vcf";
    private static final String OPTION_FILE_VEP_JSON = "vepjson";
    private static final String OPTION_START_POSITION = "start";
    private static final String OPTION_FILE_OUTPUT = "output";

    public final Path config;

    public final String caseName;
    public final Path pathFam;
    public final Path pathFamSampleName;
    public final Path pathVepFilteredVcf;
    public final Path pathVepFilteredVepJson;
    public final Path pathOutput;

    public final int start;

    public AnnotatorArgumentParser(String[] args) throws InterruptedException {
        Options options = new Options()
                .addOption(Option.builder()
                        .longOpt(OPTION_FILE_CONFIG)
                        .hasArg(true)
                        .optionalArg(false)
                        .desc("Absolute path to config file")
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

            config = Paths.get(cmd.getOptionValue(OPTION_FILE_CONFIG));

            Path dir = Paths.get("").toAbsolutePath();

            String strCaseName = cmd.getOptionValue(OPTION_CASE_NAME);
            if (strCaseName != null) {
                caseName = strCaseName;
            } else {
                caseName = dir.getFileName().toString();
            }

            String strPathFamFile = cmd.getOptionValue(OPTION_FILE_FAM);
            if (strPathFamFile != null) {
                pathFam = Paths.get(strPathFamFile).toAbsolutePath();
            } else {
                pathFam = dir.resolve(String.format("%s.fam", caseName)).toAbsolutePath();
            }

            String strPathFamSampleName = cmd.getOptionValue(OPTION_FILE_FAM_NAME);
            if (strPathFamSampleName != null) {
                pathFamSampleName = Paths.get(strPathFamSampleName).toAbsolutePath();
            } else {
                pathFamSampleName = null;
            }

            String strPathVepFilteredVepJson = cmd.getOptionValue(OPTION_FILE_VEP_JSON);
            if (strPathVepFilteredVepJson != null) {
                this.pathVepFilteredVepJson = Paths.get(strPathVepFilteredVepJson);
            } else {
                this.pathVepFilteredVepJson = null;
            }

            String strPathVepFilteredVcf = cmd.getOptionValue(OPTION_FILE_VCF);
            if (strPathVepFilteredVcf != null) {
                this.pathVepFilteredVcf = Paths.get(strPathVepFilteredVcf);
            } else {
                if (this.pathVepFilteredVepJson == null) {
                    throw new IllegalArgumentException("Missing vcf file");
                }
                strPathVepFilteredVcf = this.pathVepFilteredVepJson.getFileName().toString().split("\\.")[0] + ".vcf";
                this.pathVepFilteredVcf = Paths.get(strPathVepFilteredVcf);;
            }

            this.start = Integer.parseInt(cmd.getOptionValue(OPTION_START_POSITION, "0"));

            this.pathOutput = Paths.get(cmd.getOptionValue(OPTION_FILE_OUTPUT));

        } catch (Throwable ex) {
            log.error("Exception: ", ex);
            new HelpFormatter().printHelp("", options);

            throw new InterruptedException();
        }
    }
}
