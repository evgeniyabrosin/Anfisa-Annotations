package org.forome.annotation.connector.anfisa;

import net.minidev.json.JSONObject;
import org.forome.annotation.connector.anfisa.struct.GtfAnfisaResult;
import org.forome.annotation.connector.anfisa.struct.Kind;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.gtf.struct.GTFRegion;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GtfAnfisaBuilder {

    private final GTFConnector gtfConnector;

    protected GtfAnfisaBuilder(GTFConnector gtfConnector) {
        this.gtfConnector = gtfConnector;
    }

    public GtfAnfisaResult build(Variant variant) {
        if (variant instanceof VariantCNV) {
            return buildCNV((VariantCNV)variant);
        } else {
            return buildVep((VariantVep) variant);
        }
    }

    public GtfAnfisaResult buildCNV(VariantCNV variant) {
        return new GtfAnfisaResult(
                new GtfAnfisaResult.RegionAndBoundary("exon", Collections.emptyList()),
                new GtfAnfisaResult.RegionAndBoundary("exon", Collections.emptyList())
        );
    }

    public GtfAnfisaResult buildVep(VariantVep variant) {
        return new GtfAnfisaResult(
                getRegion(variant, Kind.CANONICAL),
                getRegion(variant, Kind.WORST)
        );
    }

    private GtfAnfisaResult.RegionAndBoundary getRegion(VariantVep variant, Kind kind) {
        //TODO Ulitin V. Отличие от python-реализации
        //Дело в том, что в оригинальной версии используется set для позиции, но в коде ниже используется итерация этому
        //списку и в конечном итоге это вляет на значение поля region - судя по всему это потенциальный баг и
        //необходима консультация с Михаилом
        List<Integer> pos = new ArrayList<>();
        pos.add(variant.start);
        if (variant.start != variant.end) {
            pos.add(variant.end);
        }

        List<JSONObject> vepTranscripts;
        if (kind == Kind.CANONICAL) {
            vepTranscripts = AnfisaConnector.getCanonicalTranscripts(variant);
        } else if (kind == Kind.WORST) {
            vepTranscripts = AnfisaConnector.getMostSevereTranscripts(variant);
        } else {
            throw new RuntimeException("Unknown kind: " + kind);
        }
        List<String> transcripts = vepTranscripts.stream()
                .filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
                .map(jsonObject -> jsonObject.getAsString("transcript_id"))
                .collect(Collectors.toList());
        if (transcripts.isEmpty()) {
            return null;
        }

        List<Object[]> distances = new ArrayList<>();
        Long dist = null;
        String region = null;
        Long index = null;
        Integer n = null;
        for (String t : transcripts) {
            dist = null;
            for (Integer p : pos) {
                Object[] result = gtfConnector.lookup(p, t);
                if (result == null) {
                    continue;
                }
                long d = (long) result[0];

                GTFRegion gtfRegion = (GTFRegion) result[1];
                region = gtfRegion.region;
                if (gtfRegion.indexRegion != null) {
                    index = gtfRegion.indexRegion;
                    n = (int) result[2];
                }

                if (dist == null || d < dist) {
                    dist = d;
                }
            }
            distances.add(
                    new Object[]{
                            dist, region, index, n
                    }
            );
        }

        return new GtfAnfisaResult.RegionAndBoundary(region, distances);
    }
}
