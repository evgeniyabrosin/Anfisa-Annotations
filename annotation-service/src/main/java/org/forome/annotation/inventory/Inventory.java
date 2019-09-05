package org.forome.annotation.inventory;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.CasePlatform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inventory {

    public final String caseName;
    public final CasePlatform casePlatform;

    public final Path famFile;
    public final Path patientIdsFile;

    public final Path vcfFile;
    public final Path vepJsonFile;

    public final Path cnvFile;

    public final Path outFile;

    public final Path logFile;

    private Inventory(
            String caseName, CasePlatform casePlatform,
            Path famFile, Path patientIdsFile,
            Path vcfFile, Path vepJsonFile,
            Path cnvFile,
            Path outFile,
            Path logFile
    ) {
        this.caseName = caseName;
        this.casePlatform = casePlatform;

        this.famFile = famFile;
        this.patientIdsFile = patientIdsFile;

        this.vcfFile = vcfFile;
        this.vepJsonFile = vepJsonFile;

        this.cnvFile = cnvFile;

        this.outFile = outFile;

        this.logFile = logFile;
    }

    public static class Builder {

        private static Pattern PATTERN_ALIAS_SPLIT = Pattern.compile(
                "^split\\('([^']*)'\\,\\s*'([^\\\"]*)'\\)$"
        );

        private String caseName;
        private CasePlatform casePlatform;

        private Path famFile;
        private Path patientIdsFile;

        private Path vcfFile;
        private Path vepJsonFile;

        private Path cnvFile;

        private Path outFile;

        public Path logFile;

        public Builder(Path file) {
            withFile(file);
        }

        public Builder withFile(Path file) {
            String fileName = file.getFileName().toString();
            String extFileName = fileName.substring(fileName.lastIndexOf(".") + 1);
            String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));

            Path dir = file.toAbsolutePath().getParent();

//            if (!fileNameWithoutExt.equals(dir.getFileName().toString()) || !"cfg".equals(extFileName)) {
//                log.warn("Improper dataset inventory path: {}", file.toAbsolutePath());
//                System.err.println("Improper dataset inventory path: " + file.toAbsolutePath());
//            }

            StringBuilder data = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("//")) {
                        continue;
                    }
                    data.append(line);
                }
            } catch (IOException e) {
                throw ExceptionBuilder.buildIOErrorException(e);
            }

            try {
                JSONObject jData = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(data.toString());

                //init aliases
                Map<String, String> aliases = new HashMap<>();
                aliases.put("NAME", fileNameWithoutExt);
                aliases.put("DIR", dir.toString());

                JSONObject jAliases = (JSONObject) jData.get("aliases");
                if (jAliases != null) {
                    for (Map.Entry<String, Object> entry : jAliases.entrySet()) {
                        String key = entry.getKey();
                        String value = (String) entry.getValue();

                        Matcher matcherSplit = PATTERN_ALIAS_SPLIT.matcher(value);
                        if (matcherSplit.matches()) {
                            String[] keys = key.split(",");
                            String[] values = getValueWithAliase(matcherSplit.group(1), aliases).split(matcherSplit.group(2));
                            for (int i = 0; i < values.length; i++) {
                                aliases.put(keys[i].trim(), values[i]);
                            }
                        } else {
                            aliases.put(key, value);
                        }
                    }
                }

                caseName = getValueWithAliase(jData.getAsString("case"), aliases);
                if (caseName == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("case");
                }

                casePlatform = CasePlatform.valueOf(getValueWithAliase(jData.getAsString("platform"), aliases).toUpperCase());

                String pathFamFile = getValueWithAliase(jData.getAsString("fam"), aliases);
                if (pathFamFile == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("fam");
                }
                famFile = Paths.get(pathFamFile).toAbsolutePath();

                String pathPatientIdsFile = getValueWithAliase(jData.getAsString("patient-ids"), aliases);
                if (pathPatientIdsFile != null) {
                    patientIdsFile = Paths.get(pathPatientIdsFile).toAbsolutePath();
                }

                String pathVcfFile = getValueWithAliase(jData.getAsString("vcf"), aliases);
                if (pathVcfFile == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("vcf");
                }
                vcfFile = Paths.get(pathVcfFile).toAbsolutePath();

                String pathVepJsonFile = getValueWithAliase(jData.getAsString("vep-json"), aliases);
                if (pathVepJsonFile != null) {
                    vepJsonFile = Paths.get(pathVepJsonFile).toAbsolutePath();
                }

                String pathCnvFile = getValueWithAliase(jData.getAsString("cnv"), aliases);
                if (pathCnvFile != null) {
                    cnvFile = Paths.get(pathCnvFile).toAbsolutePath();
                }

                String pathOutFile = getValueWithAliase(jData.getAsString("a-json"), aliases);
                if (pathOutFile == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("a-json");
                }
                outFile = Paths.get(pathOutFile).toAbsolutePath();

                String pathLogFile = getValueWithAliase(jData.getAsString("anno-log"), aliases);
                if (pathLogFile != null) {
                    logFile = Paths.get(pathLogFile).toAbsolutePath();
                }

            } catch (AnnotatorException ae) {
                throw ae;
            } catch (Throwable ex) {
                throw ExceptionBuilder.buildInvalidInventoryException(ex);
            }

            return this;
        }

        private String getValueWithAliase(String value, Map<String, String> aliases) {
            if (value == null) {
                return null;
            }
            String result = value.trim();
            if (result.isEmpty()) {
                return null;
            }

            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                String pattern = Pattern.quote("${" + entry.getKey() + "}");
                result = result.replaceAll(pattern, entry.getValue());
            }
            return result;
        }

        public Inventory build() {
            return new Inventory(
                    caseName, casePlatform,
                    famFile, patientIdsFile,
                    vcfFile, vepJsonFile,
                    cnvFile,
                    outFile,
                    logFile
            );
        }
    }
}
