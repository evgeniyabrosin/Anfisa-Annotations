package org.forome.annotation.annotator.struct;

import io.reactivex.Observable;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.struct.Sample;
import pro.parseq.vcf.VcfExplorer;
import pro.parseq.vcf.types.VcfFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotatorResult {

    public static class Metadata {

        public static class Versions {
            public final String pipelineDate = null;
            public final String annotationsDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            public final String pipeline;
            public final String annotations = AnfisaConnector.VERSION;
            public final String reference;

            public Versions(VcfExplorer vcfExplorer) {
                if (vcfExplorer!=null) {
                    VcfFile vcfFile = vcfExplorer.getVcfData();
                    pipeline = vcfFile.getOtherMetadata().get("source")
                            .stream().map(metadata -> metadata.getValue()).collect(Collectors.joining(", "));
                    reference = vcfFile.getOtherMetadata().get("reference")
                            .stream().map(metadata -> metadata.getValue()).findFirst().get();
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

        public Metadata(String caseSequence, VcfExplorer vcfExplorer, Map<String, Sample> samples) {
            this.caseSequence = caseSequence;
            this.samples = samples;
            this.versions = new Versions(vcfExplorer);
        }

        public static Metadata build(String caseSequence, VcfExplorer vcfExplorer, Map<String, Sample> samples) {
            return new Metadata(caseSequence, vcfExplorer, samples);
        }

    }

    public final Metadata metadata;
    public final Observable<AnfisaResult> observableAnfisaResult;

    public AnnotatorResult(Metadata metadata, Observable<AnfisaResult> observableAnfisaResult) {
        this.metadata = metadata;
        this.observableAnfisaResult = observableAnfisaResult;
    }
}
