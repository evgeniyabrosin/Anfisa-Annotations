package org.forome.annotation.annotator.struct;

import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import io.reactivex.Observable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.struct.mcase.Cohort;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.utils.AppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatorResult {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorResult.class);

    public static class Metadata {

        public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.systemDefault());

        public static class Versions {

            private static Pattern PATTERN_GATK_VERSION = Pattern.compile(
                    "^<ID=(ApplyRecalibration|CombineVariants),Version=(.*?)[,](.*)$", Pattern.CASE_INSENSITIVE
            );

            public final Instant pipelineDate;
            public final String pipeline;
            public final String annotations;
            public final String annotationsBuild;
            public final String reference;

            public final List<DatabaseConnector.Metadata> metadataDatabases;

            private final Map<String, String> toolGatks;
            private final Map<String, String> toolBcfs;

            public Versions(Path pathVepVcf, AnfisaConnector anfisaConnector) {
                annotations = AppVersion.getVersionFormat();
                annotationsBuild = AppVersion.getVersion();
                toolGatks = new HashMap<String, String>();
                toolBcfs = new HashMap<String, String>();
                if (pathVepVcf != null) {
                    VCFFileReader vcfFileReader = new VCFFileReader(pathVepVcf, false);
                    VCFHeader vcfHeader = vcfFileReader.getFileHeader();

                    VCFHeaderLine hlPipeline = vcfHeader.getOtherHeaderLine("source");
                    pipeline = (hlPipeline != null) ? hlPipeline.getValue() : null;

                    VCFHeaderLine hlReference = vcfHeader.getOtherHeaderLine("reference");
                    reference = (hlReference != null) ? hlReference.getValue() : null;

                    VCFHeaderLine hlPipelineDate = vcfHeader.getOtherHeaderLine("fileDate");
                    if (hlPipelineDate != null) {
                        try {
                            pipelineDate = new SimpleDateFormat("yyyyMMdd").parse(hlPipelineDate.getValue()).toInstant();
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        pipelineDate = null;
                    }

                    VCFHeaderLine hlGatkCV = vcfHeader.getMetaDataLine("GATKCommandLine.CombineVariants");
                    if (hlGatkCV != null) {
                        Matcher matcher = PATTERN_GATK_VERSION.matcher(hlGatkCV.getValue());
                        if (!matcher.matches()) {
                            throw new RuntimeException("Not support format GATK version: " + hlGatkCV.getValue());
                        }
                        toolGatks.put("combine_variants", matcher.group(2));
                    }
                    VCFHeaderLine hlGatkAR = vcfHeader.getMetaDataLine("GATKCommandLine.ApplyRecalibration");
                    if (hlGatkAR != null) {
                        Matcher matcher = PATTERN_GATK_VERSION.matcher(hlGatkAR.getValue());
                        if (!matcher.matches()) {
                            throw new RuntimeException("Not support format GATK version: " + hlGatkAR.getValue());
                        }
                        toolGatks.put("apply_recalibration", matcher.group(2));
                    }

                    VCFHeaderLine hlBCFAnnotateVersion = vcfHeader.getMetaDataLine("bcftools_annotateVersion");
                    if (hlBCFAnnotateVersion != null) {
                        toolBcfs.put("annotate", hlBCFAnnotateVersion.getValue());
                    }
                } else {
                    pipeline = null;
                    reference = null;
                    pipelineDate = null;
                }

                metadataDatabases = new ArrayList<>();
                metadataDatabases.addAll(anfisaConnector.clinvarConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.hgmdConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.spliceAIConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.conservationConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.gnomadConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.gtexConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.pharmGKBConnector.getMetadata());
                metadataDatabases.sort(Comparator.comparing(o -> o.product));
            }

            private JSONObject toJSON() {
                JSONObject out = new JSONObject();
                out.put("pipeline_date", (pipelineDate != null) ? DATE_TIME_FORMATTER.format(pipelineDate) : null);
                out.put("annotations_date", DATE_TIME_FORMATTER.format(Instant.now()));
                out.put("pipeline", pipeline);
                out.put("annotations", annotations);
                out.put("annotations_build", annotationsBuild);
                out.put("reference", reference);
                for (DatabaseConnector.Metadata metadata : metadataDatabases) {
                    StringBuilder value = new StringBuilder();
                    if (metadata.version != null) {
                        value.append(metadata.version);
                        if (metadata.date != null) {
                            value.append(" | ");
                        }
                    }
                    if (metadata.date != null) {
                        value.append(DATE_TIME_FORMATTER.format(metadata.date));
                    }
                    out.put(metadata.product, value.toString());
                }
                if (!toolGatks.isEmpty()) {
                    out.put("gatk", toolGatks);
                }
                if (!toolBcfs.isEmpty()) {
                    out.put("bcftools", toolBcfs);
                }
                return out;
            }
        }

        public final String recordType = "metadata";
        public final String caseSequence;
        public final MCase mCase;
        public final Versions versions;

        public Metadata(String caseSequence, Path pathVepVcf, MCase mCase, AnfisaConnector anfisaConnector) {
            this.caseSequence = caseSequence;
            this.mCase = mCase;
            this.versions = new Versions(pathVepVcf, anfisaConnector);
        }

        public static Metadata build(String caseSequence, Path pathVepVcf, MCase samples, AnfisaConnector anfisaConnector) {
            return new Metadata(caseSequence, pathVepVcf, samples, anfisaConnector);
        }

        public JSONObject toJSON() {
            JSONObject out = new JSONObject();
            out.put("case", caseSequence);
            out.put("record_type", recordType);
            out.put("versions", versions.toJSON());
            out.put("proband", mCase.proband.id);
            out.put("samples", new JSONObject() {{
                for (Sample sample : mCase.samples.values()) {
                    put(sample.name, build(sample));
                }
            }});
            out.put("cohorts", new JSONArray() {{
                for (Cohort cohort : mCase.cohorts) {
                    add(new JSONObject() {{
                        put("name", cohort.name);
                        put("members", new JSONArray() {{
                            for (Sample sample : cohort.getSamples()) {
                                add(sample.name);
                            }
                        }});
                    }});
                }
            }});
            return out;
        }

        public static JSONObject build(Sample sample) {
            JSONObject out = new JSONObject();
            out.put("affected", sample.affected);
            out.put("name", sample.name);
            out.put("family", sample.family);
            out.put("father", sample.father);
            out.put("sex", sample.sex);
            out.put("mother", sample.mother);
            out.put("id", sample.id);
            return out;
        }
    }

    public final Metadata metadata;
    public final Observable<AnfisaResult> observableAnfisaResult;

    public AnnotatorResult(Metadata metadata, Observable<AnfisaResult> observableAnfisaResult) {
        this.metadata = metadata;
        this.observableAnfisaResult = observableAnfisaResult;
    }
}
