package org.forome.annotation.annotator.struct;

import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import io.reactivex.Observable;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.struct.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class AnnotatorResult {

    private final static Logger log = LoggerFactory.getLogger(AnnotatorResult.class);

    public static class Metadata {

        public static class Versions {
            public final String pipelineDate = null;
            public final String annotationsDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            public final String pipeline;
            public final String annotations = AnfisaConnector.VERSION;
            public final String reference;

            public Versions(Path pathVepVcf) {
                if (pathVepVcf != null) {
                    VCFFileReader vcfFileReader = new VCFFileReader(pathVepVcf, false);
                    VCFHeader vcfHeader = vcfFileReader.getFileHeader();
                    pipeline = vcfHeader.getOtherHeaderLine("source").getValue();
                    reference = vcfHeader.getOtherHeaderLine("reference").getValue();
                } else {
                    pipeline = null;
                    reference = null;
                }
            }
        }

        public final String recordType = "metadata";
        public final String caseSequence;
        public final Map<String, Sample> samples;
        public final Versions versions;

        public Metadata(String caseSequence, Path pathVepVcf, Map<String, Sample> samples) {
            this.caseSequence = caseSequence;
            this.samples = samples;
            this.versions = new Versions(pathVepVcf);
        }

        public static Metadata build(String caseSequence, Path pathVepVcf, Map<String, Sample> samples) {
            return new Metadata(caseSequence, pathVepVcf, samples);
        }

    }

    public final Metadata metadata;
    public final Observable<AnfisaResult> observableAnfisaResult;

    public AnnotatorResult(Metadata metadata, Observable<AnfisaResult> observableAnfisaResult) {
        this.metadata = metadata;
        this.observableAnfisaResult = observableAnfisaResult;
    }
}
