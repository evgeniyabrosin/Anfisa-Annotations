package org.forome.annotation.annotator.main.argument;

import org.apache.commons.cli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgumentsAnnotation extends Arguments {

    public final Path config;

    public final String caseName;
    public final Path pathFam;
    public final Path pathFamSampleName;
    public final Path pathVcf;
    public final Path pathVepJson;
    public final Path pathOutput;

    public final int start;

    public ArgumentsAnnotation(CommandLine cmd) {
        super(cmd);

        config = Paths.get(cmd.getOptionValue(ParserArgument.OPTION_FILE_CONFIG));

        Path dir = Paths.get("").toAbsolutePath();

        String strCaseName = cmd.getOptionValue(ParserArgument.OPTION_CASE_NAME);
        if (strCaseName != null) {
            caseName = strCaseName;
        } else {
            caseName = dir.getFileName().toString();
        }

        String strPathFamFile = cmd.getOptionValue(ParserArgument.OPTION_FILE_FAM);
        if (strPathFamFile != null) {
            pathFam = Paths.get(strPathFamFile).toAbsolutePath();
        } else {
            pathFam = dir.resolve(String.format("%s.fam", caseName)).toAbsolutePath();
        }

        String strPathFamSampleName = cmd.getOptionValue(ParserArgument.OPTION_FILE_FAM_NAME);
        if (strPathFamSampleName != null) {
            pathFamSampleName = Paths.get(strPathFamSampleName).toAbsolutePath();
        } else {
            pathFamSampleName = null;
        }

        String strPathVepJson = cmd.getOptionValue(ParserArgument.OPTION_FILE_VEP_JSON);
        if (strPathVepJson != null) {
            this.pathVepJson = Paths.get(strPathVepJson);
        } else {
            this.pathVepJson = null;
        }

        String strPathVcf = cmd.getOptionValue(ParserArgument.OPTION_FILE_VCF);
        if (strPathVcf != null) {
            this.pathVcf = Paths.get(strPathVcf);
        } else {
            if (this.pathVepJson == null) {
                throw new IllegalArgumentException("Missing vcf file");
            }
            strPathVcf = this.pathVepJson.getFileName().toString().split("\\.")[0] + ".vcf";
            this.pathVcf = Paths.get(strPathVcf);
        }

        this.start = Integer.parseInt(cmd.getOptionValue(ParserArgument.OPTION_START_POSITION, "0"));

        this.pathOutput = Paths.get(cmd.getOptionValue(ParserArgument.OPTION_FILE_OUTPUT));
    }

}
