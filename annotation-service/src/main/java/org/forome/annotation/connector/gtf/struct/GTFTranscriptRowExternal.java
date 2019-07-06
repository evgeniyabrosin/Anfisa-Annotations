package org.forome.annotation.connector.gtf.struct;

public class GTFTranscriptRowExternal extends GTFTranscriptRow {

    public final String transcript;

    public final String approved;

    public GTFTranscriptRowExternal(
            String transcript, String gene, String approved,
            long start, long end,
            String feature
    ) {
        super(gene, start, end, feature);

        this.transcript = transcript;
        this.approved = approved;
    }
}
