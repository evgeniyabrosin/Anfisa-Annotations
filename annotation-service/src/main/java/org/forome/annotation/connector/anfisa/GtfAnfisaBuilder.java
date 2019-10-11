package org.forome.annotation.connector.anfisa;

import net.minidev.json.JSONObject;
import org.forome.annotation.connector.anfisa.struct.GtfAnfisaResult;
import org.forome.annotation.connector.anfisa.struct.Kind;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.gtf.struct.GTFRegion;
import org.forome.annotation.connector.gtf.struct.GTFTranscriptRow;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GtfAnfisaBuilder {

    private final GTFConnector gtfConnector;

    protected GtfAnfisaBuilder(GTFConnector gtfConnector) {
        this.gtfConnector = gtfConnector;
    }

    public GtfAnfisaResult build(Variant variant) {
        if (variant instanceof VariantCNV) {
            return buildCNV((VariantCNV) variant);
        } else {
            return buildVep((VariantVep) variant);
        }
    }

    /**
     * Логика работы:
     * В CNV-файл переходят только те мутации, которые задевают какой-то экзон, и дистанция получается всегда 0,
     * но для надежности, мы проверяем, что cnv-варианта, не входит полностью в какойто экзон, алгоритм:
     * 1) По пробегаем по каждому транскрипту
     * 2) В нем пробегаем по каждому входящему в него экзому
     * 3) Проверяем, что вариант не помещается ни в какой экзом, если помещается, то вычисляем минимальное расстояние,
     * между краями экзона и краями cnv-варианта
     *
     * @param variant
     * @return
     */
    public GtfAnfisaResult buildCNV(VariantCNV variant) {
        return new GtfAnfisaResult(
                getRegionByCNV(variant, Kind.CANONICAL),
                getRegionByCNV(variant, Kind.WORST)
        );
    }

    private GtfAnfisaResult.RegionAndBoundary getRegionByCNV(VariantCNV variant, Kind kind) {
        List<JSONObject> vepTranscripts = getVepTranscripts(variant, kind);
        List<String> transcripts = vepTranscripts.stream()
                .filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
                .map(jsonObject -> jsonObject.getAsString("transcript_id"))
                .collect(Collectors.toList());
        if (transcripts.isEmpty()) {
            return null;
        }

        List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distances = new ArrayList<>();
        for (String transcript : transcripts) {
            List<GTFTranscriptRow> transcriptRows = gtfConnector.getTranscriptRows(transcript);

            GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distance = null;
            for (int index = 0; index < transcriptRows.size(); index++) {
                GTFTranscriptRow transcriptRow = transcriptRows.get(index);
                if (transcriptRow.start <= variant.start && variant.end <= transcriptRow.end) {
                    int minDist = Math.min(variant.start - transcriptRow.start, transcriptRow.end - variant.end);
                    if (distance == null || distance.dist > minDist) {
                        distance = new GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary(
                                minDist, "exon", index, transcriptRows.size()
                        );
                    }
                }
            }
            if (distance != null) {
                distances.add(distance);
            }
        }
        return new GtfAnfisaResult.RegionAndBoundary("exon", distances);
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

        List<JSONObject> vepTranscripts = getVepTranscripts(variant, kind);
        List<String> transcripts = vepTranscripts.stream()
                .filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
                .map(jsonObject -> jsonObject.getAsString("transcript_id"))
                .collect(Collectors.toList());
        if (transcripts.isEmpty()) {
            return null;
        }

        List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distances = new ArrayList<>();
        String region = null;
        Integer index = null;
        Integer n = null;
        for (String t : transcripts) {
            Long dist = null;
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
            if (dist != null){
                distances.add(
                        new GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary(
                                dist, region, index, n
                        )
                );
            }
        }

        return new GtfAnfisaResult.RegionAndBoundary(region, distances);
    }

    private List<JSONObject> getVepTranscripts(VariantVep variant, Kind kind) {
        if (kind == Kind.CANONICAL) {
            return AnfisaConnector.getCanonicalTranscripts(variant);
        } else if (kind == Kind.WORST) {
            return AnfisaConnector.getMostSevereTranscripts(variant);
        } else {
            throw new RuntimeException("Unknown kind: " + kind);
        }
    }
}
