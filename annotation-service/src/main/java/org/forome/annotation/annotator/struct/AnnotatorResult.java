package org.forome.annotation.annotator.struct;

import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import io.reactivex.Observable;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.utils.AppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class AnnotatorResult {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorResult.class);

    public static class Metadata {

        public static class Versions {

            public final String pipelineDate = null;
            public final String annotationsDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            public final String pipeline;
            public final String annotations;
            public final String annotationsBuild;
            public final String reference;

            public final List<DatabaseConnector.Metadata> metadataDatabases;

            public Versions(Path pathVepVcf, AnfisaConnector anfisaConnector) {
                annotations = AppVersion.getVersionFormat();
                annotationsBuild = AppVersion.getVersion();
                if (pathVepVcf != null) {
                    VCFFileReader vcfFileReader = new VCFFileReader(pathVepVcf, false);
                    VCFHeader vcfHeader = vcfFileReader.getFileHeader();

                    VCFHeaderLine hlPipeline = vcfHeader.getOtherHeaderLine("source");
                    pipeline = (hlPipeline!=null)?hlPipeline.getValue():null;

                    VCFHeaderLine hlReference = vcfHeader.getOtherHeaderLine("reference");
                    reference = (hlReference!=null)?hlReference.getValue():null;
                } else {
                    pipeline = null;
                    reference = null;
                }

                metadataDatabases = new ArrayList<>();
                metadataDatabases.addAll(anfisaConnector.clinvarConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.hgmdConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.spliceAIConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.conservationConnector.getMetadata());
                metadataDatabases.addAll(anfisaConnector.gnomadConnector.getMetadata());
                metadataDatabases.sort(Comparator.comparing(o -> o.product));
            }
        }

        public final String recordType = "metadata";
        public final String caseSequence;
        public final Map<String, Sample> samples;
        public final Versions versions;

        public Metadata(String caseSequence, Path pathVepVcf, Map<String, Sample> samples, AnfisaConnector anfisaConnector) {
            this.caseSequence = caseSequence;
            this.samples = samples;
            this.versions = new Versions(pathVepVcf, anfisaConnector);
        }

        public static Metadata build(String caseSequence, Path pathVepVcf, Map<String, Sample> samples, AnfisaConnector anfisaConnector) {
            return new Metadata(caseSequence, pathVepVcf, samples, anfisaConnector);
        }

    }

    public final Metadata metadata;
    public final Observable<AnfisaResult> observableAnfisaResult;

    public AnnotatorResult(Metadata metadata, Observable<AnfisaResult> observableAnfisaResult) {
        this.metadata = metadata;
        this.observableAnfisaResult = observableAnfisaResult;
    }
}
