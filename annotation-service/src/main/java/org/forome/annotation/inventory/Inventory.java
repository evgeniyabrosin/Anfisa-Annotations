package org.forome.annotation.inventory;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.CasePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Inventory {

    private final static Logger log = LoggerFactory.getLogger(Inventory.class);

    public final String caseName;
    public final CasePlatform casePlatform;

    public final Path famFile;
    public final Path patientIdsFile;

    public final Path vcfFile;
    public final Path vepJsonFile;

    public final Path outFile;

    private Inventory(
            String caseName, CasePlatform casePlatform,
            Path famFile, Path patientIdsFile,
            Path vcfFile, Path vepJsonFile,
            Path outFile
    ) {
        this.caseName = caseName;
        this.casePlatform = casePlatform;

        this.famFile = famFile;
        this.patientIdsFile = patientIdsFile;

        this.vcfFile = vcfFile;
        this.vepJsonFile = vepJsonFile;

        this.outFile = outFile;
    }

    public static class Builder {

        private String caseName;
        private CasePlatform casePlatform;

        private Path famFile;
        private Path patientIdsFile;

        private Path vcfFile;
        private Path vepJsonFile;

        private Path outFile;

        public Builder(Path file) {
            withFile(file);
        }

        public Builder withFile(Path file) {
            String fileName = file.getFileName().toString();
            String extFileName = fileName.substring(fileName.lastIndexOf(".") + 1);
            String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));

            Path dir = file.toAbsolutePath().getParent();

            if (!fileNameWithoutExt.equals(dir.getFileName().toString()) || !"inv".equals(extFileName)) {
                log.warn("Improper dataset inventory path: {}", file.toAbsolutePath());
                System.err.println("Improper dataset inventory path: " + file.toAbsolutePath());
            }

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
                        aliases.put(entry.getKey(), (String) entry.getValue());
                    }
                }

                caseName = getValueWithAliase(jData.getAsString("name"), aliases);
                if (caseName == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("name");
                }

                casePlatform = CasePlatform.valueOf(jData.getAsString("platform").toUpperCase());

                String pathFamFile = getValueWithAliase(jData.getAsString("fam"), aliases);
                if (pathFamFile == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("fam");
                }
                famFile = Paths.get(pathFamFile).toAbsolutePath();
//                if (!Files.exists(famFile)) {
//                    throw ExceptionBuilder.buildInvalidValueInventoryException("fam", "File is not exists: " + famFile);
//                }

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

                String pathOutFile = getValueWithAliase(jData.getAsString("a-json"), aliases);
                if (pathOutFile == null) {
                    throw ExceptionBuilder.buildInvalidValueInventoryException("a-json");
                }
                outFile = Paths.get(pathOutFile).toAbsolutePath();

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
                    outFile
            );
        }
    }
}
