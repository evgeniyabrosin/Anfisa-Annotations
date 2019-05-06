package org.forome.annotation.connector.anfisa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.connector.anfisa.struct.*;
import org.forome.annotation.connector.beacon.BeaconConnector;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.clinvar.struct.ClinvarResult;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;
import org.forome.annotation.connector.gtf.GTFConnector;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.connector.spliceai.SpliceAIConnector;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.exception.ServiceException;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.struct.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnfisaConnector implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(AnfisaConnector.class);

    public static String VERSION = "0.3.4";

    private static final Map<String, String> trustedSubmitters = new HashMap<String, String>() {{
        put("lmm", "Laboratory for Molecular Medicine,Partners HealthCare Personalized Medicine");
        put("gene_dx", "GeneDx");
    }};

    private static final ImmutableList<String> csq_damaging = ImmutableList.of(
            "transcript_ablation", "splice_acceptor_variant", "splice_donor_variant", "stop_gained", "frameshift_variant",
            "stop_lost", "start_lost", "transcript_amplification", "inframe_insertion", "inframe_deletion"
    );

    private static final Map<String, String> proteins_3_to_1 = new HashMap<String, String>() {{
        put("Ala", "A");
        put("Arg", "R");
        put("Asn", "N");
        put("Asp", "D");
        put("Cys", "C");
        put("Gln", "Q");
        put("Glu", "E");
        put("Gly", "G");
        put("His", "H");
        put("Ile", "I");
        put("Leu", "L");
        put("Lys", "K");
        put("Met", "M");
        put("Phe", "F");
        put("Pro", "P");
        put("Ser", "S");
        put("Thr", "T");
        put("Trp", "W");
        put("Tyr", "Y");
        put("Val", "V");

    }};


    private final ImmutableList<String> csq_missense = ImmutableList.of("missense_variant");

    private final GnomadConnector gnomadConnector;
    private final SpliceAIConnector spliceAIConnector;
    private final HgmdConnector hgmdConnector;
    private final ClinvarConnector clinvarConnector;
    private final LiftoverConnector liftoverConnector;
    private final GTFConnector gtfConnector;

    private final AnfisaHttpClient anfisaHttpClient;

    public AnfisaConnector(
            GnomadConnector gnomadConnector,
            SpliceAIConnector spliceAIConnector,
            HgmdConnector hgmdConnector,
            ClinvarConnector clinvarConnector,
            LiftoverConnector liftoverConnector,
            GTFConnector gtfConnector
    ) throws IOException {
        this.gnomadConnector = gnomadConnector;
        this.spliceAIConnector = spliceAIConnector;
        this.hgmdConnector = hgmdConnector;
        this.clinvarConnector = clinvarConnector;
        this.liftoverConnector = liftoverConnector;
        this.anfisaHttpClient = new AnfisaHttpClient();
        this.gtfConnector = gtfConnector;
    }

    public CompletableFuture<List<AnfisaResult>> request(String chromosome, long start, long end, String alternative) {
        String region = String.format("%s:%s:%s", chromosome, start, end);
        String endpoint = String.format("/vep/human/region/%s/%s?hgvs=true&canonical=true&merged=true&protein=true&variant_class=true", region, alternative);

        return anfisaHttpClient.request(endpoint).thenApply(jsonArray -> {
            List<AnfisaResult> result = new ArrayList<>();
            for (Object item : jsonArray) {
                result.add(build(null, chromosome, start, end, (JSONObject) item, null, null));
            }
            return result;
        });
    }

    public AnfisaResult build(
            String caseSequence,
            String chromosome,
            long start,
            long end,
            JSONObject json,
            VariantContext variantContext,
            Map<String, Sample> samples
    ) {
        Record record = new Record();

        AnfisaResultFilters filters = new AnfisaResultFilters();
        AnfisaResultData data = new AnfisaResultData();
        AnfisaResultView view = new AnfisaResultView();

        data.version = getVersion();

        callGnomAD(variantContext, samples, json, filters);
        callSpliceai(data, filters, variantContext, samples, json);
        callHgmd(record, chromosome, start, end, filters, data);
        callClinvar(record, chromosome, start, end, variantContext, samples, filters, data, view, json);
        callBeacon(variantContext, samples, json, data);
        GtfAnfisaResult gtfAnfisaResult = callGtf(start, end, json);
        callQuality(filters, variantContext, samples);

        filters.severity = getSeverity(json);

        String proband = getProband(samples);
        if (proband != null) {
            String mother = samples.get(proband).mother;
            String father = samples.get(proband).father;
            data.zygosity = new HashMap<>();
            filters.altZygosity = new HashMap<>();
            for (Map.Entry<String, Sample> entry : samples.entrySet()) {
                String name = entry.getValue().name;
                int sex = entry.getValue().sex;
                String label;
                if (entry.getKey().equals(proband)) {
                    label = String.format("proband [%s]", name);
                } else if (entry.getKey().equals(mother)) {
                    label = String.format("mother [%s]", name);
                } else if (entry.getKey().equals(father)) {
                    label = String.format("father [%s]", name);
                } else {
                    label = entry.getKey();
                }

                int zyg = sampleHasVariant(json, variantContext, samples, entry.getValue());
                data.zygosity.put(entry.getKey(), zyg);
                int modified_zygosity = (!chromosome.equals("X") || sex == 2 || zyg == 0) ? zyg : 2;
                filters.altZygosity.put(entry.getKey(), modified_zygosity);
                if (zyg > 0) {
                    filters.has_variant.add(label);
                }
            }
        }

        List<Object> d = getDistanceFromExon(gtfAnfisaResult, json, "worst");
        filters.distFromExon = d.stream()
                .filter(o -> (o instanceof Number))
                .map(o -> ((Number) o).longValue())
                .min(Long::compareTo).orElse(0L);

        filters.chromosome = (chromosome.length() < 2) ? String.format("chr%s", chromosome) : getChromosome(json);

        data.assemblyName = json.getAsString("assembly_name");
        data.end = json.getAsNumber("end").longValue();
        data.regulatoryFeatureConsequences = (JSONArray) json.get("regulatory_feature_consequences");
        data.motifFeatureConsequences = (JSONArray) json.get("motif_feature_consequences");
        data.intergenicConsequences = (JSONArray) json.get("intergenic_consequences");
        data.start = json.getAsNumber("start").longValue();
        data.mostSevereConsequence = json.getAsString("most_severe_consequence");
        data.alleleString = json.getAsString("allele_string");
        data.seqRegionName = json.getAsString("seq_region_name");
        data.colocatedVariants = (JSONArray) json.get("colocated_variants");
        data.input = json.getAsString("input");
        data.label = getLabel(variantContext, samples, json);
        data.transcriptConsequences = (JSONArray) json.get("transcript_consequences");
        data.id = json.getAsString("id");
        data.strand = (json.containsKey("strand")) ? json.getAsNumber("strand").longValue() : null;
        data.variantClass = (json.containsKey("variant_class")) ? json.getAsString("variant_class") : null;

        data.colorCode = getColorCode(json, data);

        data.distFromBoundaryCanonical = gtfAnfisaResult.distFromBoundaryCanonical;
        data.regionCanonical = gtfAnfisaResult.regionCanonical;
        data.distFromBoundaryWorst = gtfAnfisaResult.distFromBoundaryWorst;
        data.regionWorst = gtfAnfisaResult.regionWorst;

        createGeneralTab(data, filters, view, start, end, json, caseSequence, variantContext, samples);
        createQualityTab(filters, view, variantContext, samples);
        createGnomadTab(chromosome, variantContext, samples, json, filters, data, view);
        createDatabasesTab(json, record, data, view);
        createPredictionsTab(json, view);
        createBioinformaticsTab(gtfAnfisaResult, json, data, view, variantContext, samples);

        return new AnfisaResult(filters, data, view);
    }

    private static String getVersion() {
        return VERSION;
    }

    private static Integer sampleHasVariant(JSONObject json, VariantContext variantContext, Map<String, Sample> samples, Sample sample) {
        if (variantContext == null) {
            return 0;
        }

        Integer idx = null;
        if (sample.name.toLowerCase().equals("proband")) {
            idx = 0;
        } else if (sample.name.toLowerCase().equals("mother")) {
            idx = 1;
        } else if (sample.name.toLowerCase().equals("father")) {
            idx = 2;
        }

        String genotype;
        if (idx == null) {
            genotype = getGtBasesGenotype(variantContext, sample.name);
        } else {
            /**
             genotypes = self.get_genotypes()
             if (len(genotypes) <= idx):
             return False
             genotype = genotypes[idx]
             */
            throw new RuntimeException("Not implemented");
        }

        if (genotype == null) {
            return 0;
        }

        Set<String> set1 = Arrays.stream(genotype.split("/")).collect(Collectors.toSet());
        Set<String> set2 = new HashSet<>(alt_list(variantContext, samples, json));
        if (!Collections.disjoint(set1, set2)) {
            return 3 - set1.size();
        }
        return 0;
    }

    private void callHgmd(Record record, String chromosome, long start, long end, AnfisaResultFilters filters, AnfisaResultData data) {
        List<String> accNums = hgmdConnector.getAccNum(chromosome, start, end);
        if (accNums.size() > 0) {
            HgmdConnector.Data hgmdData = hgmdConnector.getDataForAccessionNumbers(accNums);
            record.hgmdData = hgmdData;

            data.hgmd = String.join(",", accNums);
            List<String[]> hg38 = hgmdConnector.getHg38(accNums);
            data.hgmdHg38 = hg38.stream().map(strings -> String.format("%s-%s", strings[0], strings[1])).collect(Collectors.joining(", "));
            List<String> tags = hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).collect(Collectors.toList());
            filters.hgmdBenign = (tags.size() == 0);
        }
    }

    private void callClinvar(Record record, String chromosome, long start, long end, VariantContext variantContext, Map<String, Sample> samples, AnfisaResultFilters filters, AnfisaResultData data, AnfisaResultView view, JSONObject json) {
        List<ClinvarResult> clinvarResults;
        if (isSnv(json)) {
            clinvarResults = clinvarConnector.getData(chromosome, start, end, alt_list(variantContext, samples, json));
        } else {
            clinvarResults = clinvarConnector.getExpandedData(chromosome, start);
        }
        record.clinvarResults = clinvarResults;
        if (clinvarResults.isEmpty()) return;

        String[] variants = clinvarResults.stream().map(clinvarResult -> {
            return String.format("%s %s>%s",
                    vstr(getChromosome(json), clinvarResult.start, clinvarResult.end),
                    clinvarResult.referenceAllele, clinvarResult.alternateAllele
            );
        }).toArray(String[]::new);

        List<String> significance = new ArrayList<>();
        Map<String, String> submissions = new HashMap<>();
        for (ClinvarResult clinvarResult : clinvarResults) {
            significance.addAll(Arrays.asList(clinvarResult.clinicalSignificance.split("/")));
            submissions.putAll(clinvarResult.submitters);
        }

        List<String> idList = clinvarResults.stream().flatMap(it -> {
            return Lists.newArrayList(it.phenotypeIDs, it.otherIDs).stream();
        }).collect(Collectors.toList());
        for (String id : idList) {
            if (id.indexOf(":") != -1) {
                continue;
            }
            //TODO not implemented
        }

        data.clinVar = clinvarResults.stream()
                .map(clinvarResult -> clinvarResult.variationID)
                .map(it -> Long.parseLong(it))
                .toArray(Long[]::new);
        data.clinvarSubmitters = new HashMap<String, String>() {{
            for (ClinvarResult clinvarResult : clinvarResults) {
                putAll(clinvarResult.submitters);
            }
        }};

        view.databases.clinVar = clinvarResults.stream()
                .map(clinvarResult -> clinvarResult.variationID)
                .map(it -> String.format("https://www.ncbi.nlm.nih.gov/clinvar/variation/%s/", it))
                .toArray(String[]::new);
        data.clinvarVariants = variants;
        view.databases.clinVarSubmitters = data.clinvarSubmitters.entrySet().stream().map(entry -> {
            return String.format("%s: %s", encodeToAscii(entry.getKey()), entry.getValue());
        }).toArray(String[]::new);
        data.clinvarSignificance = significance.toArray(new String[significance.size()]);
        data.clinvarPhenotypes = clinvarResults.stream()
                .map(clinvarResult -> clinvarResult.phenotypeList)
                .toArray(String[]::new);

        filters.clinvarBenign = (significance.stream().filter(s -> (s.toLowerCase().indexOf("benign") == -1)).count() == 0);

        Boolean benign = null;
        for (String submitter : trustedSubmitters.keySet()) {
            String fullName = trustedSubmitters.get(submitter);
            if (submissions.containsKey(fullName)) {
                String prediction = submissions.get(fullName).toLowerCase();
                data.setField(submitter, prediction);
                if (!prediction.contains("benign")) {
                    benign = false;
                } else if (benign == null) {
                    benign = true;
                }
            }
        }
        filters.clinvarTrustedBenign = Optional.ofNullable(benign);
    }

    private void callBeacon(VariantContext variantContext, Map<String, Sample> samples, JSONObject json, AnfisaResultData data) {
        List<String> alts = alt_list(variantContext, samples, json);
        data.beaconUrls = alts.stream()
                .map(alt ->
                        BeaconConnector.getUrl(
                                RequestParser.toChromosome(getChromosome(json)),
                                json.getAsNumber("start").longValue(),
                                getRef(variantContext, json), alt
                        )
                )
                .toArray(String[]::new);
    }

    private GtfAnfisaResult callGtf(long start, long end, JSONObject json) {
        GtfAnfisaResult gtfAnfisaResult = new GtfAnfisaResult();

        //TODO Ulitin V. Отличие от python-реализации
        //Дело в том, что в оригинальной версии используется set для позиции, но в коде ниже используется итерация этому
        //списку и в конечном итоге это вляет на значение поля region - судя по всему это потенциальный баг и
        //необходима консультация с Михаилом
        List<Long> pos = new ArrayList<>();
        pos.add(start);
        if (start != end) {
            pos.add(end);
        }

        List<String> transcriptKinds = Lists.newArrayList("canonical", "worst");
        for (String kind : transcriptKinds) {
            List<String> transcripts;
            if ("canonical".equals(kind)) {
                transcripts = getCanonicalTranscripts(json).stream()
                        .filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
                        .map(jsonObject -> jsonObject.getAsString("transcript_id"))
                        .collect(Collectors.toList());
            } else if ("worst".equals(kind)) {
                transcripts = getMostSevereTranscripts(json).stream()
                        .filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
                        .map(jsonObject -> jsonObject.getAsString("transcript_id"))
                        .collect(Collectors.toList());
            } else {
                throw new RuntimeException("Unknown Transcript Kind: " + kind);
            }
            if (transcripts.isEmpty()) {
                continue;
            }

            List<Object[]> distances = new ArrayList<>();
            Long dist = null;
            String region = null;
            Long index = null;
            Integer n = null;
            for (String t : transcripts) {
                dist = null;
                for (Long p : pos) {
                    Object[] result = gtfConnector.lookup(p, t);
                    if (result == null) {
                        continue;
                    }
                    long d = (long) result[0];
                    region = (String) result[1];
                    if (result.length > 3) {
                        index = (long) result[2];
                        n = (int) result[3];
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

            if ("canonical".equals(kind)) {
                gtfAnfisaResult.distFromBoundaryCanonical = distances;
                gtfAnfisaResult.regionCanonical = region;
            } else if ("worst".equals(kind)) {
                gtfAnfisaResult.distFromBoundaryWorst = distances;
                gtfAnfisaResult.regionWorst = region;
            } else {
                throw new RuntimeException("Unknown Transcript Kind: " + kind);
            }
        }
        return gtfAnfisaResult;
    }

    private static void callQuality(AnfisaResultFilters filters, VariantContext variantContext, Map<String, Sample> samples) {
        filters.minGq = getMinGQ(variantContext, samples);
        filters.probandGq = getProbandGQ(variantContext, samples);
        if (variantContext != null) {
            CommonInfo commonInfo = variantContext.getCommonInfo();
            filters.qd = toPrimitiveDouble(commonInfo.getAttribute("QD"));
            filters.fs = toPrimitiveDouble(commonInfo.getAttribute("FS"));
            filters.mq = toDouble(commonInfo.getAttribute("MQ"));

            if (variantContext.isFiltered()) {
                filters.filters = new ArrayList<>(variantContext.getFilters());
            } else {
                filters.filters = Lists.newArrayList("PASS");
            }
        }
    }


    private void callGnomAD(VariantContext variantContext, Map<String, Sample> samples, JSONObject response, AnfisaResultFilters filters) {
        Double af = null;
        Double _af = null;
        Double emAf = null;
        Double emAfPb = null;
        Double gmAf = null;
        Double gmAfPb = null;
        Double _afPb = null;

        String popmax = null;
        Double popmaxAf = null;
        Long popmaxAn = null;

        Long hom = null;
        Long hem = null;

        for (String alt : alt_list(variantContext, samples, response)) {
            GnomadResult gnomadResult = getGnomadResult(variantContext, response, alt);
            if (gnomadResult == GnomadResult.EMPTY) {
                continue;
            }
            if (gnomadResult.exomes != null) {
                af = gnomadResult.exomes.af;
                emAf = Math.min((emAf != null && emAf != 0.0d) ? emAf : af, af);
                if (isProbandHasAllele(variantContext, samples, alt)) {
                    emAfPb = Math.min((emAfPb != null) ? emAfPb : af, af);
                }
            }
            if (gnomadResult.genomes != null) {
                af = gnomadResult.genomes.af;
                gmAf = Math.min((gmAf != null) ? gmAf : af, af);
                if (isProbandHasAllele(variantContext, samples, alt)) {
                    gmAfPb = Math.min((gmAfPb != null) ? gmAfPb : af, af);
                }
            }

            af = gnomadResult.overall.af;
            if (isProbandHasAllele(variantContext, samples, alt)) {
                _afPb = Math.min((_afPb != null) ? _afPb : af, af);
            }

            if (hom == null || hom < gnomadResult.overall.hom) {
                hom = gnomadResult.overall.hom;
            }
            if (hem == null || hem < gnomadResult.overall.hem) {
                hem = gnomadResult.overall.hem;
            }

            if (_af == null || af < _af) {
                _af = af;
                popmax = gnomadResult.popmax;
                popmaxAf = gnomadResult.popmaxAf;
                popmaxAn = gnomadResult.popmaxAn;
            }
        }

        filters.gnomadDbExomesAf = emAf;
        filters.gnomadDbGenomesAf = gmAf;
        filters.gnomadAfFam = _af;
        filters.gnomadAfPb = _afPb;
        filters.gnomadHom = hom;
        filters.gnomadHem = hem;

        filters.gnomadPopmax = popmax;
        filters.gnomadPopmaxAf = popmaxAf;
        filters.gnomadPopmaxAn = popmaxAn;
    }

    private GnomadResult getGnomadResult(VariantContext variantContext, JSONObject response, String alt) {
        try {
            return gnomadConnector.request(
                    RequestParser.toChromosome(getChromosome(response)),
                    Math.min(response.getAsNumber("start").longValue(), response.getAsNumber("end").longValue()),
                    getRef(variantContext, response), alt
            ).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    private void callSpliceai(AnfisaResultData data, AnfisaResultFilters filters, VariantContext variantContext, Map<String, Sample> samples, JSONObject json) {
        SpliceAIResult spliceAIResult = spliceAIConnector.getAll(
                RequestParser.toChromosome(getChromosome(json)),
                lowest_coord(json),
                ref(json, variantContext),
                alt_list(variantContext, samples, json)
        );
        data.spliceAI = spliceAIResult.dict_sql;
        filters.spliceAltering = spliceAIResult.cases;
        filters.spliceAiDsmax = spliceAIResult.max_ds;
    }

    private void createGeneralTab(AnfisaResultData data, AnfisaResultFilters filters, AnfisaResultView view, long start, long end, JSONObject json, String caseSequence, VariantContext variantContext, Map<String, Sample> samples) {
        view.general.genes = getGenes(json).stream().toArray(String[]::new);
        view.general.hg19 = str(variantContext, samples, json);
        view.general.hg38 = getHg38Coordinates(json);

        if (isSnv(json)) {
            data.ref = getRef(variantContext, json);
            data.alt = altString(variantContext, samples, json);
        } else {
            view.general.ref = getRef(variantContext, json);
            view.general.alt = altString(variantContext, samples, json);
        }

        List<String>[] cPosTpl = getPosTpl(json, "c");
        view.general.cposWorst = cPosTpl[0];
        view.general.cposCanonical = cPosTpl[1];
        view.general.cposOther = cPosTpl[2];

        List<String>[] pPosTpl = getPosTpl(json, "p");
        view.general.pposWorst = pPosTpl[0];
        view.general.pposCanonical = pPosTpl[1];
        view.general.pposOther = pPosTpl[2];

        Object[] gGenotypes = getGenotypes(variantContext, samples);
        view.general.probandGenotype = (String) gGenotypes[0];
        view.general.maternalGenotype = (String) gGenotypes[1];
        view.general.paternalGenotype = (String) gGenotypes[2];

        view.general.worstAnnotation = data.mostSevereConsequence;
        List<String> consequenceTerms = getFromCanonicalTranscript(json, "consequence_terms");
        String canonicalAnnotation = getMostSevere(consequenceTerms);
        if (consequenceTerms.size() > 1) {
            String finalCanonicalAnnotation = canonicalAnnotation;
            List<String> otherTerms = consequenceTerms.stream()
                    .filter(s -> !s.equals(finalCanonicalAnnotation))
                    .collect(Collectors.toList());
            canonicalAnnotation = String.format("%s [%s]", canonicalAnnotation, String.join(", ", otherTerms));
        }
        view.general.canonicalAnnotation = canonicalAnnotation;

        view.general.spliceRegion = getFromTranscripts(json, "spliceregion", "all");
        view.general.geneSplicer = getFromTranscripts(json, "genesplicer", "all");

        List<JSONObject> transcripts = getMostSevereTranscripts(json);
        view.general.refseqTranscriptWorst = getFromTranscripts(transcripts, "transcript_id", "RefSeq");
        view.general.ensemblTranscriptsWorst = getFromTranscripts(transcripts, "transcript_id", "Ensembl");

        transcripts = getCanonicalTranscripts(json);
        view.general.refseqTranscriptCanonical = getFromTranscripts(transcripts, "transcript_id", "RefSeq");
        view.general.ensemblTranscriptsCanonical = getFromTranscripts(transcripts, "transcript_id", "Ensembl");

        view.general.variantExonWorst = getFromWorstTranscript(json, "exon");
        view.general.variantIntronWorst = getFromWorstTranscript(json, "intron");
        view.general.variantExonCanonical = getFromCanonicalTranscript(json, "exon");
        view.general.variantIntronCanonical = getFromCanonicalTranscript(json, "intron");

        String[] intronOrExonCanonical = getIntronOrExon(json, "canonical");
        data.variantExonIntronCanonical = intronOrExonCanonical[0];
        data.totalExonIntronCanonical = intronOrExonCanonical[1];

        String[] intronOrExonWorst = getIntronOrExon(json, "worst");
        data.variantExonIntronWorst = intronOrExonWorst[0];
        data.totalExonIntronWorst = intronOrExonWorst[1];

        view.general.igv = getIgvUrl(start, end, json, caseSequence, samples);

        if (filters.spliceAiDsmax != null) {
            if (filters.spliceAiDsmax >= SpliceAIConnector.MAX_DS_UNLIKELY) {
                view.general.spliceAltering = Optional.ofNullable(getSpliceAltering(filters));
            }
        } else {
            view.general.spliceAltering = Optional.empty();
        }
    }

    private static String getSpliceAltering(AnfisaResultFilters filters) {
        return filters.spliceAltering;
    }

    private static String[] getIntronOrExon(JSONObject json, String kind) {
        List<String> introns = getFromTranscripts(json, "intron", kind);
        List<String> exons = getFromTranscripts(json, "exon", kind);
        List<String> e = (exons.size() > 0) ? exons : introns;
        if (e.size() == 0) {
            return new String[]{null, null};
        }

        List<String> index = new ArrayList<>();
        List<String> total = new ArrayList<>();
        for (String x : e) {
            if (x == null) continue;
            String[] split = x.split("/");
            index.add(split[0]);
            if (split.length > 1) {
                total.add(split[1]);
            }
        }

        return new String[]{
                String.join(",", index),
                String.join(",", total)
        };
    }


    private void createQualityTab(AnfisaResultFilters filters, AnfisaResultView view, VariantContext variantContext, Map<String, Sample> samples) {
        if (variantContext == null) return;
        CommonInfo commonInfo = variantContext.getCommonInfo();
        if (commonInfo == null) return;

        JSONObject q_all = new JSONObject();
        q_all.put("title", "All");
        q_all.put("strand_odds_ratio", toDouble(commonInfo.getAttribute("SOR")));
        q_all.put("mq", toDouble(commonInfo.getAttribute("MQ")));

        q_all.put("variant_call_quality", variantContext.getPhredScaledQual());
        q_all.put("qd", toDouble(commonInfo.getAttribute("QD")));
        q_all.put("fs", toDouble(commonInfo.getAttribute("FS")));
        q_all.put("ft", (commonInfo.isFiltered())
                ? commonInfo.getFilters().stream().collect(Collectors.toList())
                : Lists.newArrayList("PASS")
        );
        view.qualitySamples.add(q_all);

        String proband = getProband(samples);
        String mother = samples.get(proband).mother;
        String father = samples.get(proband).father;
        for (Map.Entry<String, Sample> entry : samples.entrySet()) {
            Sample sample = entry.getValue();
            String s = sample.name;
            JSONObject q_s = new JSONObject();
            if (s.equals(proband)) {
                q_s.put("title", String.format("Proband: %s", s));
            } else if (s.equals(mother)) {
                q_s.put("title", String.format("Mother: %s", s));
            } else if (s.equals(father)) {
                q_s.put("title", String.format("Father: %s", s));
            } else {
                q_s.put("title", s);
            }

            Genotype oGenotype = variantContext.getGenotype(sample.name);
            q_s.put("allelic_depth", oGenotype.getAnyAttribute("AD"));
            q_s.put("read_depth", oGenotype.getAnyAttribute("DP"));
            q_s.put("genotype_quality", getVariantGQ(variantContext, sample));
            view.qualitySamples.add(q_s);
        }
    }

    private void createGnomadTab(String chromosome, VariantContext variantContext, Map<String, Sample> samples, JSONObject json, AnfisaResultFilters filters, AnfisaResultData data, AnfisaResultView view) {
        Double gnomadAf = getGnomadAf(filters);
        if (gnomadAf != null && Math.abs(gnomadAf) > 0.000001D) {
            for (String allele : alt_list(variantContext, samples, json)) {
                GnomadResult gnomadResult = getGnomadResult(variantContext, json, allele);

                AnfisaResultView.GnomAD gnomAD = new AnfisaResultView.GnomAD();
                gnomAD.allele = allele;
                gnomAD.pli = getPLIByAllele(json, allele);

                if (gnomadResult.exomes != null) {
                    gnomAD.exomeAn = gnomadResult.exomes.an;
                    gnomAD.exomeAf = gnomadResult.exomes.af;
                }
                if (gnomadResult.genomes != null) {
                    gnomAD.genomeAn = gnomadResult.genomes.an;
                    gnomAD.genomeAf = gnomadResult.genomes.af;
                }

                gnomAD.proband = (isProbandHasAllele(variantContext, samples, allele)) ? "Yes" : "No";
                if (gnomadResult.overall != null) {
                    gnomAD.af = gnomadResult.overall.af;
                    gnomAD.hom = gnomadResult.overall.hom;
                    gnomAD.hem = gnomadResult.overall.hem;
                }
                gnomAD.popMax = String.format("%s: %s [%s]", gnomadResult.popmax, gnomadResult.popmaxAf, gnomadResult.popmaxAn);
                gnomAD.url = gnomadResult.urls.stream().map(url -> url.toString()).toArray(String[]::new);

                view.gnomAD.add(gnomAD);
            }
        } else {
            int p1 = lowest_coord(json) - 2;
            int p2 = highest_coord(json) + 1;

            AnfisaResultView.GnomAD gnomAD = new AnfisaResultView.GnomAD();
            gnomAD.url = new String[]{
                    String.format("https://gnomad.broadinstitute.org/region/%s-%s-%s", chromosome, p1, p2)
            };
            view.gnomAD.add(gnomAD);
        }
    }

    private static int lowest_coord(JSONObject json) {
        return Math.min(start(json), end(json));
    }

    private static int highest_coord(JSONObject json) {
        return Math.max(start(json), end(json));
    }

    private static int start(JSONObject json) {
        return json.getAsNumber("start").intValue();
    }

    private static int end(JSONObject json) {
        return json.getAsNumber("end").intValue();
    }

    private static List<Double> getPLIByAllele(JSONObject json, String allele) {
        List<JSONObject> transcripts = getTranscripts(json, "protein_coding");
        String key = "exacpli";
        List<Double> list = unique(
                transcripts.stream()
                        .filter(jsonObject -> jsonObject.containsKey(key))
                        .filter(jsonObject -> allele.equals(jsonObject.get("variant_allele")))
                        .map(jsonObject -> jsonObject.getAsNumber(key).doubleValue())
                        .collect(Collectors.toList())
        );
        if (list.size() > 0) {
            return list;
        }
        return null;
    }

    private void createDatabasesTab(JSONObject response, Record record, AnfisaResultData data, AnfisaResultView view) {
        if (data.hgmd != null) {
            view.databases.hgmd = data.hgmd;
            view.databases.hgmdHg38 = data.hgmdHg38;

            view.databases.hgmdTags = record.hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).toArray(String[]::new);
            if (view.databases.hgmdTags.length == 0) view.databases.hgmdTags = null;

            view.databases.hgmdPhenotypes = record.hgmdData.phenotypes.toArray(new String[record.hgmdData.phenotypes.size()]);
            if (view.databases.hgmdPhenotypes.length == 0) view.databases.hgmdPhenotypes = null;

            view.databases.hgmdPmids = record.hgmdData.hgmdPmidRows.stream()
                    .map(hgmdPmidRow -> hgmdPmidRow.pmid).map(pmid -> linkToPmid(pmid)).toArray(String[]::new);
            if (view.databases.hgmdPmids.length == 0) view.databases.hgmdPmids = null;

            data.hgmdPmids = record.hgmdData.hgmdPmidRows.stream()
                    .map(hgmdPmidRow -> hgmdPmidRow.pmid).toArray(String[]::new);
            if (data.hgmdPmids.length == 0) data.hgmdPmids = null;
        } else {
            view.databases.hgmd = "Not Present";
        }
        view.databases.beaconUrl = data.beaconUrls;

        if (data.clinvarVariants != null) {
            view.databases.clinVarVariants = Arrays.stream(data.clinvarVariants).distinct().toArray(String[]::new);
        }
        if (data.clinvarSignificance != null) {
            view.databases.clinVarSignificance = Arrays.stream(data.clinvarSignificance).distinct().toArray(String[]::new);
        }
        if (data.clinvarPhenotypes != null) {
            view.databases.clinVarPhenotypes = Arrays.stream(data.clinvarPhenotypes).distinct().toArray(String[]::new);
        }
        for (String submitter : trustedSubmitters.keySet()) {
            view.databases.setField(String.format("%s_significance", submitter), data.getField(submitter));
        }
        view.databases.pubmedSearch = getTenwiseLink(response);
        view.databases.omim = getGenes(response).stream().map(gene ->
                String.format("https://omim.org/search/?search=approved_gene_symbol:%s&retrieve=geneMap", gene)
        ).toArray(String[]::new);
        view.databases.geneCards = getGenes(response).stream().map(gene ->
                String.format("https://www.genecards.org/cgi-bin/carddisp.pl?gene=%s", gene)
        ).toArray(String[]::new);
    }

    private void createPredictionsTab(JSONObject json, AnfisaResultView view) {
        view.predictions.lofScore = getFromTranscripts(json, "loftool", "all")
                .stream().map(s -> Double.parseDouble(s)).sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        view.predictions.lofScoreCanonical = getFromCanonicalTranscript(json, "loftool")
                .stream().map(s -> Double.parseDouble(s)).sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        view.predictions.maxEntScan = getMaxEnt(json);

        view.predictions.polyphen = getFromTranscriptsList(json, "polyphen_prediction").stream().toArray(String[]::new);
        view.predictions.polyphen2Hvar = getFromTranscriptsList(json, "Polyphen2_HVAR_pred".toLowerCase()).stream().collect(Collectors.toList());
        view.predictions.polyphen2Hdiv = getFromTranscriptsList(json, "Polyphen2_HDIV_pred".toLowerCase()).stream().collect(Collectors.toList());
        view.predictions.polyphen2HvarScore = getFromTranscriptsList(json, "Polyphen2_HVAR_score".toLowerCase()).stream()
                .collect(Collectors.toList());
        view.predictions.polyphen2HdivScore = getFromTranscriptsList(json, "Polyphen2_HDIV_score".toLowerCase()).stream()
                .collect(Collectors.toList());
        view.predictions.sift = getFromTranscriptsList(json, "sift_prediction").stream().toArray(String[]::new);
        view.predictions.siftScore = getFromTranscriptsList(json, "sift_score").stream().toArray(String[]::new);
        view.predictions.revel = getFromTranscriptsList(json, "revel_score").stream().map(s -> Double.parseDouble(s))
                .collect(Collectors.toList());
        view.predictions.mutationTaster = getFromTranscriptsList(json, "mutationtaster_pred").stream().toArray(String[]::new);
        view.predictions.fathmm = getFromTranscriptsList(json, "fathmm_pred").stream().toArray(String[]::new);
        view.predictions.caddPhred = getFromTranscriptsList(json, "cadd_phred").stream().map(s -> Double.parseDouble(s))
                .collect(Collectors.toList());
        view.predictions.caddRaw = getFromTranscriptsList(json, "cadd_raw").stream().map(s -> Double.parseDouble(s))
                .collect(Collectors.toList());
        view.predictions.mutationAssessor = getFromTranscriptsList(json, "mutationassessor_pred").stream().toArray(String[]::new);
    }

    private static List<String> getMaxEnt(JSONObject json) {
        List<JSONObject> transcripts = getTranscripts(json, "protein_coding");
        Set<String> x = new HashSet<>();
        for (JSONObject transcript : transcripts) {
            Number m1 = transcript.getAsNumber("maxentscan_ref");
            Number m2 = transcript.getAsNumber("maxentscan_alt");
            Number m3 = transcript.getAsNumber("maxentscan_diff");
            if (m1 != null && m2 != null && m3 != null) {
                String v = String.format("%s=%s-%s", m3, m1, m2);
                x.add(v);
            }
        }
        if (!x.isEmpty()) {
            return new ArrayList<String>(x);
        } else {
            return null;
        }
    }

    private static void createBioinformaticsTab(GtfAnfisaResult gtfAnfisaResult, JSONObject json, AnfisaResultData data, AnfisaResultView view, VariantContext variantContext, Map<String, Sample> samples) {
        view.bioinformatics.zygosity = getZygosity(json, variantContext, samples);
        view.bioinformatics.inheritedFrom = inherited_from(json, variantContext, samples);
        view.bioinformatics.distFromExonWorst = getDistanceFromExon(gtfAnfisaResult, json, "worst");
        view.bioinformatics.distFromExonCanonical = getDistanceFromExon(gtfAnfisaResult, json, "canonical");
        view.bioinformatics.conservation = unique(getFromTranscriptsList(json, "conservation")).stream()
                .map(s -> Double.parseDouble(s)).collect(Collectors.toList());
        view.bioinformatics.speciesWithVariant = "";
        view.bioinformatics.speciesWithOthers = "";
        view.bioinformatics.maxEntScan = getMaxEnt(json);
        view.bioinformatics.nnSplice = "";
        view.bioinformatics.humanSplicingFinder = "";
        view.bioinformatics.otherGenes = getOtherGenes(json);
        view.bioinformatics.calledBy = getCallers(json, variantContext, samples).stream().toArray(String[]::new);
        view.bioinformatics.callerData = getCallersData(variantContext);
        view.bioinformatics.spliceAi = list_dsmax(data);

        String[] splice_ai_keys = new String[]{"AG", "AL", "DG", "DL"};
        if (!data.spliceAI.isEmpty()) {
            for (Map.Entry<String, SpliceAIResult.DictSql> entry : data.spliceAI.entrySet()) {
                for (String s : splice_ai_keys) {
                    String key = String.format("DS_%s", s);
                    float score = entry.getValue().getValue(key).floatValue();
                    if (score > 0.0f) {
                        String key2 = String.format("DP_%s", s);
                        int position = entry.getValue().getValue(key2).intValue();
                        String sPosition = String.valueOf(position);
                        if (position > 0) {
                            sPosition = "+" + sPosition;
                        }
                        view.bioinformatics.getSpliceAiValues(s).add(
                                String.format("%s: %s[%s]", entry.getKey(), String.format(Locale.ENGLISH, "%.4f", score), sPosition)
                        );
                    }
                }
            }
        }
    }

    private static Map<String, Float> list_dsmax(AnfisaResultData data) {
        Map<String, Float> result = new HashMap<>();
        if (data.spliceAI.isEmpty()) {
            return result;
        }
        result.put(
                "DS_AG",
                data.spliceAI.values().stream().map(dictSql -> dictSql.ds_ag).max(Float::compareTo).get()
        );
        result.put(
                "DS_AL",
                data.spliceAI.values().stream().map(dictSql -> dictSql.ds_al).max(Float::compareTo).get()
        );
        result.put(
                "DS_DG",
                data.spliceAI.values().stream().map(dictSql -> dictSql.ds_dg).max(Float::compareTo).get()
        );
        result.put(
                "DS_DL",
                data.spliceAI.values().stream().map(dictSql -> dictSql.ds_dl).max(Float::compareTo).get()
        );
        return result;
    }

    private static String[] getOtherGenes(JSONObject response) {
        Set<String> genes = new HashSet<>(getGenes(response));
        Set<String> allGenes = new HashSet<>(getFromTranscriptsByBiotype(response, "gene_symbol", "all"));

        Set<String> result = new HashSet<>();
        result.addAll(allGenes);
        result.removeAll(genes);
        return result.toArray(new String[result.size()]);
    }

    private String getHg38Coordinates(JSONObject response) {
        String chromosome = getChromosome(response);
        String c = RequestParser.toChromosome(chromosome);

        Integer hg38Start = liftoverConnector.hg38(
                c,
                response.getAsNumber("start").longValue()
        );
        Integer hg38End = liftoverConnector.hg38(
                c,
                response.getAsNumber("end").longValue()
        );
        if (Objects.equals(hg38Start, hg38End)) {
            return String.format("%s:%s", chromosome, (hg38Start != null) ? hg38Start : "None");
        } else {
            return String.format("%s:%s-%s", chromosome,
                    (hg38Start != null) ? hg38Start : "None",
                    (hg38End != null) ? hg38End : "None"
            );
        }
    }

    private static String getProband(Map<String, Sample> samples) {
        if (samples == null) {
            return null;
        }
        for (Map.Entry<String, Sample> entry : samples.entrySet()) {
            if (entry.getValue().id.endsWith("a1")) {
                return entry.getValue().id;
            }
        }
        return null;
    }

    public String getColorCode(JSONObject response, AnfisaResultData data) {
        List<String> pp = getFromTranscriptsList(response, "polyphen_prediction");
        List<String> ss = getFromTranscriptsList(response, "sift_prediction");

        String best = null;
        String worst = null;
        for (String p : pp) {
            if (p.contains("benign")) {
                best = "B";
            } else if (p.contains("possibly_damaging")) {
                if (!"D".equals(worst)) {
                    worst = "PD";
                }
            } else if (p.contains("damaging")) {
                worst = "D";
            }
        }
        for (String s : ss) {
            if (s.contains("tolerated")) {
                best = "B";
            }
            if (s.contains("deleterious")) {
                worst = "D";
            }
        }

        String code = null;
        if (!"B".equals(best) && "D".equals(worst)) {
            code = "red";
        } else if ("B".equals(best) && worst == null) {
            code = "green";
        } else if (best != null || worst != null) {
            code = "yellow";
        }
        if (code != null) return code;

        String csq = data.mostSevereConsequence;
        if (csq_damaging.contains(csq)) {
            code = "red-cross";
        } else if (csq_missense.contains(csq)) {
            code = "yellow-cross";
        }

        return code;
    }

    private String[] getTenwiseLink(JSONObject response) {
        List<String> hgncIds = getHgncIds(response);
        return hgncIds.stream().map(hgncId ->
                String.format("https://www.tenwiseapps.nl/publicdl/variant_report/HGNC_%s_variant_report.html", hgncId)
        ).toArray(String[]::new);
    }

    public String altString(VariantContext variantContext, Map<String, Sample> samples, JSONObject response) {
        return String.join(",", alt_list(variantContext, samples, response));
    }

    private static List<String> alt_list(VariantContext variantContext, Map<String, Sample> samples, JSONObject response) {
        if (variantContext != null) {
            List<String> alleles = variantContext.getAlleles()
                    .stream().map(allele -> allele.getBaseString()).collect(Collectors.toList());
            List<String> alt_allels = variantContext.getAlternateAlleles()
                    .stream().map(allele -> allele.getBaseString()).collect(Collectors.toList());
            if (samples != null) {
                Map<String, Long> counts = new HashMap<>();
                for (Genotype genotype : variantContext.getGenotypes()) {
                    int[] ad = genotype.getAD();
                    if (ad == null || ad.length == 0) {
                        return alt_allels;
                    }
                    for (int i = 0; i < alleles.size(); i++) {
                        String al = alleles.get(i);
                        long n = ad[i];
                        counts.put(al, counts.getOrDefault(al, 0L) + n);
                    }
                }
                List<String> tmp_alt_allels = alt_allels.stream()
                        .filter(s -> counts.containsKey(s) && counts.get(s) > 0)
                        .collect(Collectors.toList());
                if (!tmp_alt_allels.isEmpty()) {
                    return tmp_alt_allels;
                } else {
                    return alt_allels;
                }
            } else {
                return alt_allels;
            }
        } else {
            return getAlts1(response);
        }
    }

    private static List<String> getAlts1(JSONObject response) {
        String[] ss = getAllele(response).split("/");
        List<String> result = new ArrayList<>();
        for (int i = 1; i < ss.length; i++) {
            result.add(ss[i]);
        }
        return result;
    }

    private static String getAllele(JSONObject response) {
        return response.getAsString("allele_string");
    }

    private static String getRef(VariantContext variantContext, JSONObject response) {
        if (variantContext != null) {
            return variantContext.getReference().getBaseString();
        } else {
            return getRef1(response);
        }
    }

    private static String getRef1(JSONObject response) {
        return response.getAsString("allele_string").split("/")[0];
    }

    private static String getChromosome(JSONObject response) {
        return response.getAsString("seq_region_name");
    }

    private static boolean isProbandHasAllele(VariantContext variantContext, Map<String, Sample> samples, String alt) {
        if (samples == null || variantContext == null) {
            return false;
        }
        String probandGenotype = (String) getGenotypes(variantContext, samples)[0];
        if (probandGenotype == null) {
            return false;
        }
        Set<String> set1 = Arrays.stream(probandGenotype.split("/")).collect(Collectors.toSet());
        return set1.contains(alt);
    }

    private static Integer getMinGQ(VariantContext variantContext, Map<String, Sample> samples) {
        if (variantContext == null) {
            return null;
        }
        Integer GQ = null;
        for (Sample s : samples.values()) {
            Integer gq = getVariantGQ(variantContext, s);
            if (gq != null && gq != 0) {
                if (GQ == null || gq < GQ) {
                    GQ = gq;
                }
            }
        }
        return GQ;
    }

    private static Integer getProbandGQ(VariantContext variantContext, Map<String, Sample> samples) {
        if (samples == null || variantContext == null) {
            return null;
        }
        return getVariantGQ(variantContext, samples.get(getProband(samples)));
    }

    private static Integer getVariantGQ(VariantContext variantContext, Sample s) {
        int valie = variantContext.getGenotype(s.name).getGQ();
        return (valie != -1) ? valie : null;
    }

    private Long getSeverity(JSONObject response) {
        String csq = response.getAsString("most_severe_consequence");
        int n = Variant.SEVERITY.size();
        for (int s = 0; s < n; s++) {
            if (Variant.SEVERITY.get(s).contains(csq)) {
                return Long.valueOf(n - s - 1);
            }
        }
        return null;
    }

    private static List<Object> getDistanceFromExon(GtfAnfisaResult gtfAnfisaResult, JSONObject json, String kind) {
        List<Object[]> distFromBoundary;
        if ("canonical".equals(kind)) {
            distFromBoundary = gtfAnfisaResult.distFromBoundaryCanonical;
        } else if ("worst".equals(kind)) {
            distFromBoundary = gtfAnfisaResult.distFromBoundaryWorst;
        } else {
            throw new RuntimeException("Unknown kind: " + kind);
        }
        if (distFromBoundary != null) {
            return unique(
                    distFromBoundary.stream().map(objects -> (Long) objects[0]).collect(Collectors.toList())
            );
        }

        return unique(
                getHgvsList(json, "c", kind).stream()
                        .map(hgvcs -> getDistanceHgvsc(hgvcs))
                        .collect(Collectors.toList()),
                "Exonic"
        );
    }

    private static List<Character> hgvs_signs = Lists.newArrayList('-', '+', '*');

    private static Long getDistanceHgvsc(String hgvcs) {
        String[] sChunks = hgvcs.split(":");
        String coord = null;
        for (int i = 1; i < sChunks.length; i++) {
            String chunk = sChunks[i];
            char ch = chunk.charAt(0);
            if (ch == 'c' || ch == 'p') {
                coord = chunk;
                break;
            }
        }
        if (coord == null) {
            return null;
        }
        String[] xx = coord.split("\\.");
        Integer d = null;
        try {
            for (String x : xx[1].split("_")) {
                Integer sign = null;
                Integer p1 = null;
                Integer p2 = null;
                while (!Character.isDigit(x.charAt(0)) && !hgvs_signs.contains(x.charAt(0))) {
                    x = x.substring(1);
                }
                int end = x.length();
                for (int i = 0; i < end; i++) {
                    char c = x.charAt(i);
                    if (Character.isDigit(c)) {
                        continue;
                    }
                    if (hgvs_signs.contains(c)) {
                        int p0 = (sign == null) ? 0 : sign + 1;
                        p1 = (i > p0) ? Integer.parseInt(x.substring(p0, i)) : 0;
                        sign = i;
                    }
                    if (Character.isAlphabetic(c)) {
                        end = i;
                        break;
                    }
                }
                if (p1 != null && sign != null) {
                    p2 = Integer.parseInt(x.substring(sign + 1, end));
                }
                if (p2 != null) {
                    if (d == null || d > p2) {
                        d = p2;
                    }
                }
            }
            return (d != null) ? d.longValue() : null;
        } catch (Exception e) {
            log.error("Exception ", e);
            return null;
        }
    }

    private static String getIgvUrl(long start, long end, JSONObject json, String caseSequence, Map<String, Sample> samples) {
        if (caseSequence == null || samples == null) {
            return null;
        }
        String url = "http://localhost:60151/load?";
        String path = "/anfisa/links/";
        String host = "anfisa.forome.org";
        List<String> fileUrls = samples.keySet().stream()
                .map(sample -> String.format("http://%s%s%s/%s.hg19.bam", host, path, caseSequence, sample))
                .collect(Collectors.toList());
        String name = String.join(",", samples.keySet());
        String args = String.format("file=%s&genome=hg19&merge=false&name=%s&locus=%s:%s-%s",
                String.join(",", fileUrls), name, getChromosome(json), start - 250, end + 250
        );
        return String.format("%s%s", url, args);
    }

    public String getLabel(VariantContext variantContext, Map<String, Sample> samples, JSONObject response) {
        List<String> genes = getGenes(response);
        String gene;
        if (genes.size() == 0) {
            gene = "None";
        } else if (genes.size() < 3) {
            gene = String.join(",", genes);
        } else {
            gene = "...";
        }

        String vstr = str(variantContext, samples, response);

        return String.format("[%s] %s", gene, vstr);
    }

    private static List<String> getGenes(JSONObject response) {
        return getFromTranscriptsList(response, "gene_symbol");
    }

    public List<String> getHgncIds(JSONObject response) {
        return getFromTranscriptsList(response, "hgnc_id");
    }

    /**
     * def get_most_severe_transcripts(self):
     * msq = self.get_msq()
     * return [t for t in self.get_transcripts() if (msq in t.get("consequence_terms"))]
     */
    private static List<JSONObject> getMostSevereTranscripts(JSONObject json) {
        String msq = getMsq(json);
        return getTranscripts(json, "protein_coding").stream()
                .filter(jsonObject -> {
                    JSONArray consequenceTerms = (JSONArray) jsonObject.get("consequence_terms");
                    return (consequenceTerms != null && consequenceTerms.contains(msq));
                })
                .collect(Collectors.toList());
    }

    private static List<JSONObject> getCanonicalTranscripts(JSONObject json) {
        return getTranscripts(json, "protein_coding").stream()
                .filter(jsonObject -> jsonObject.containsKey("canonical"))
                .collect(Collectors.toList());
    }


    private static List<String> getFromTranscriptsList(JSONObject json, String key) {
        return getFromTranscriptsByBiotype(json, key, "protein_coding");
    }

    private static List<JSONObject> getTranscripts(JSONObject response, String biotype) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray jTranscriptConsequences = (JSONArray) response.get("transcript_consequences");
        if (jTranscriptConsequences != null) {
            for (Object oItem : jTranscriptConsequences) {
                JSONObject item = (JSONObject) oItem;
                if (biotype == null || biotype.toUpperCase().equals("ALL")) {
                    result.add(item);
                } else if (item.get("biotype").equals(biotype)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static List<String> getFromTranscriptsByBiotype(JSONObject response, String key, String biotype) {
        List<String> result = new ArrayList<>();

        for (JSONObject item : getTranscripts(response, biotype)) {
            Object oValue = item.get(key);
            if (oValue == null) continue;
            if (oValue instanceof JSONArray) {
                for (Object io : (JSONArray) oValue) {
                    String i = String.valueOf(io);
                    if (!result.contains(i)) {
                        result.add(i);
                    } else {
                        //TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
                        //Дело в том, что в python реализации косвенно получался такой результат,
                        //непонятно, это сделано специально или нет, если не важна сортировака, то заменить на обычный Set
                        result.remove(i);
                        result.add(0, i);
                    }
                }
            } else {
                String value = String.valueOf(oValue);
                if (!result.contains(value)) {
                    result.add(value);
                } else {
                    //TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
                    //Дело в том, что в python реализации косвенно получался такой результат,
                    //непонятно, это сделано специально или нет, если не важна сортировака, то заменить на обычный Set
                    result.remove(value);
                    result.add(0, value);
                }
            }
        }
        return result;
    }

    private static List<String> getFromWorstTranscript(JSONObject json, String key) {
        return unique(
                getMostSevereTranscripts(json).stream()
                        .filter(jsonObject -> jsonObject.containsKey(key))
                        .map(jsonObject -> jsonObject.getAsString(key))
                        .collect(Collectors.toList())
        );
    }

    private static List<String> getFromCanonicalTranscript(JSONObject json, String key) {
        return unique(
                getCanonicalTranscripts(json).stream()
                        .filter(jsonObject -> jsonObject.containsKey(key))
                        .flatMap(jsonObject -> {
                            Object value = jsonObject.get(key);
                            if (value instanceof String) {
                                return Stream.of((String) value);
                            } else if (value instanceof Number) {
                                return Stream.of(String.valueOf((Number) value));
                            } else {
                                return ((JSONArray) value)
                                        .stream()
                                        .map(o -> (String) o);
                            }
                        })
                        .collect(Collectors.toList())
        );
    }

    private static List<String> getFromTranscripts(List<JSONObject> transcripts, String key, String source) {
        return unique(
                transcripts.stream()
                        .filter(jsonObject -> source.equals(jsonObject.getAsString("source")))
                        .map(jsonObject -> jsonObject.getAsString(key))
                        .collect(Collectors.toList())
        );
    }

    private static List<String> getFromTranscripts(JSONObject json, String key, String type) {
        if ("all".equals(type)) {
            return getFromTranscriptsList(json, key);
        } else if ("canonical".equals(type)) {
            return getFromCanonicalTranscript(json, key);
        } else if ("worst".equals(type)) {
            return getFromWorstTranscript(json, key);
        } else {
            throw new RuntimeException("Unknown type: " + type);
        }
    }

    private static List<String> getHgvsList(JSONObject json, String type, String kind) {
        if ("c".equals(type)) {
            return getFromTranscripts(json, "hgvsc", kind);
        } else if ("p".equals(type)) {
            return getFromTranscripts(json, "hgvsp", kind);
        } else {
            List<String> result = new ArrayList<>();
            result.addAll(getFromTranscripts(json, "hgvsc", kind));
            result.addAll(getFromTranscripts(json, "hgvsp", kind));
            return result;
        }
    }

    private static String hgvcsPos(String str, String type, boolean withPattern) {
        String pattern = String.format(":%s.", type);
        if (!str.contains(str)) {
            return null;
        }
        String x = str.split(pattern)[1];
        if ("p".equals(type)) {
            x = convert_p(x);
        }

        if (withPattern) {
            return String.format("%s%s", pattern.substring(1), x);
        } else {
            return x;
        }
    }

    private static String convert_p(String x) {
        List<Character> protein1 = new ArrayList<>();
        List<Character> pos = new ArrayList<>();
        List<Character> protein2 = new ArrayList<>();
        int state = 0;
        for (char c : x.toCharArray()) {
            if (state == 0) {
                if (Character.isLetter(c)) {
                    protein1.add(c);
                    continue;
                }
                state = 2;
            }
            if (state == 2) {
                if (Character.isDigit(c)) {
                    pos.add(c);
                    continue;
                }
                state = 3;
            }
            if (state == 3) {
                protein2.add(c);
            } else {
                break;
            }
        }
        String p1 = protein1.stream().map(c -> c.toString()).collect(Collectors.joining());
        String p2 = protein2.stream().map(c -> c.toString()).collect(Collectors.joining());
        String rpos = pos.stream().map(c -> c.toString()).collect(Collectors.joining());
        String rprotein1 = proteins_3_to_1.getOrDefault(p1, p1);
        String rprotein2 = proteins_3_to_1.getOrDefault(p2, p2);
        return String.format("%s%s%s", rprotein1, rpos, rprotein2);
    }

    private static List<String> getPos(JSONObject json, String type, String kind) {
        List<String> hgvsList = getHgvsList(json, type, kind);
        List<String> poss = hgvsList.stream()
                .map(hgvcs -> hgvcsPos(hgvcs, type, true))
                .collect(Collectors.toList());
        return unique(poss);
    }

    private static List<String>[] getPosTpl(JSONObject json, String type) {
        Set<String> ss = new HashSet<>();

        List<String> c_worst = getPos(json, type, "worst");
        ss.addAll(c_worst);

        List<String> c_canonical = getPos(json, type, "canonical");
        ss.addAll(c_canonical);

        List<String> c_other = getPos(json, type, "all");
        if (c_other.isEmpty()) {
            c_other = Collections.emptyList();
        } else {
            ss.removeAll(c_other);
            c_other = (ss.isEmpty()) ? null : new ArrayList<>(ss);
        }

        return new List[]{
                c_worst, c_canonical, c_other
        };
    }

    public String str(VariantContext variantContext, Map<String, Sample> samples, JSONObject json) {
        String str = getHg19Coordinates(json);
        if (isSnv(json)) {
            return String.format("%s  %s>%s", str, ref(json, variantContext), altString(variantContext, samples, json));
        } else {
            String variantClass = getVariantClass(json);
            return String.format("%s %s", str, (variantClass != null) ? variantClass : "None");
        }
    }

    public String getHg19Coordinates(JSONObject response) {
        return vstr(
                getChromosome(response),
                response.getAsNumber("start").longValue(),
                response.getAsNumber("end").longValue()
        );
    }

    public String vstr(String c, long s, long e) {
        if (s == e) {
            return String.format("%s:%s", c, s);
        } else {
            return String.format("%s:%s-%s", c, s, e);
        }
    }

    private static String getMsq(JSONObject json) {
        return json.getAsString("most_severe_consequence");
    }

    public boolean isSnv(JSONObject json) {
        return "SNV".equals(getVariantClass(json));
    }

    private String getVariantClass(JSONObject json) {
        return json.getAsString("variant_class");
    }

    public Double getGnomadAf(AnfisaResultFilters filters) {
        return filters.gnomadAfFam;
    }

    public String linkToPmid(String pmid) {
        return String.format("https://www.ncbi.nlm.nih.gov/pubmed/%s", pmid);
    }

    private static String encodeToAscii(String s) {
        String regex = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";
        try {
            return new String(s.replaceAll(regex, "").getBytes("ascii"), "ascii");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String ref(JSONObject json, VariantContext variantContext) {
        if (variantContext != null) {
            return variantContext.getReference().getBaseString();
        } else {
            return ref1(json);
        }
    }

    private static String ref1(JSONObject json) {
        String s = json.getAsString("allele_string");
        return s.split("/")[0];
    }

    private static Object[] getGenotypes(VariantContext variantContext, Map<String, Sample> samples) {
        String empty = "Can not be determined";
        String proband = getProband(samples);
        if (proband == null) {
            return new Object[]{null, null, null, null};
        }
        String probandGenotype = getGtBasesGenotype(variantContext, proband);
        if (probandGenotype == null) {
            probandGenotype = empty;
        }

        String mother = samples.get(proband).mother;
        if ("0".equals(mother)) {
            mother = null;
        }
        String maternalGenotype = (mother != null) ? getGtBasesGenotype(variantContext, mother) : null;
        if (mother != null && maternalGenotype == null) {
            maternalGenotype = empty;
        }

        String father = samples.get(proband).father;
        if ("0".equals(father)) {
            father = null;
        }
        String paternalGenotype = (father != null) ? getGtBasesGenotype(variantContext, father) : null;
        if (father != null && paternalGenotype == null) {
            paternalGenotype = empty;
        }

        String finalProbandGenotype = probandGenotype;
        String finalMaternalGenotype = maternalGenotype;
        String finalPaternalGenotype = paternalGenotype;
        List<String> otherGenotypes = samples.keySet().stream()
                .map(genotype -> getGtBasesGenotype(variantContext, genotype))
                .filter(gtBases -> gtBases != null)
                .filter(gtBases -> !gtBases.equals(finalProbandGenotype))
                .filter(gtBases -> !gtBases.equals(finalMaternalGenotype))
                .filter(gtBases -> !gtBases.equals(finalPaternalGenotype))
                .distinct()
                .collect(Collectors.toList());

        return new Object[]{probandGenotype, maternalGenotype, paternalGenotype, otherGenotypes};
    }

    private static String getGtBasesGenotype(VariantContext variantContext, String genotype) {
        if (variantContext == null) {
            return null;
        }

        Genotype oGenotype = variantContext.getGenotype(genotype);
        if (oGenotype.isCalled()) {
            return oGenotype.getGenotypeString();
        } else {
            return null;
        }
    }

    private static String getMostSevere(List<String> consequenceTerms) {
        for (String item : Variant.CONSEQUENCES) {
            if (consequenceTerms.contains(item)) {
                return item;
            }
        }
        return null;
    }

    private static List<String> getRawCallers(VariantContext variantContext) {
        List<String> callers = new ArrayList<>();
        CommonInfo commonInfo = variantContext.getCommonInfo();
        for (String caller : Variant.CALLERS) {
            if (commonInfo.hasAttribute(caller)) {
                if (!callers.contains(caller)) {
                    callers.add(caller);
                }
            }
        }
        return callers;
    }


    private static List<String> getCallers(JSONObject json, VariantContext variantContext, Map<String, Sample> samples) {
        if (samples == null || variantContext == null) {
            return Collections.emptyList();
        }
        List<String> callers = getRawCallers(variantContext);

        Object[] genotypes = getGenotypes(variantContext, samples);
        String probandGenotype = (String) genotypes[0];
        String maternalGenotype = (String) genotypes[1];
        String paternalGenotype = (String) genotypes[2];
        if (probandGenotype == null || maternalGenotype == null || paternalGenotype == null) {
            return callers;
        }

        String ref = ref(json, variantContext);
        List<String> alt_set = alt_list(variantContext, samples, json);
        Set<String> p_set = Arrays.stream(probandGenotype.split("/")).collect(Collectors.toSet());
        Set<String> m_set = Arrays.stream(maternalGenotype.split("/")).collect(Collectors.toSet());
        Set<String> f_set = Arrays.stream(paternalGenotype.split("/")).collect(Collectors.toSet());

        for (String alt : alt_set) {
            if (p_set.contains(alt) && !m_set.contains(alt) && !f_set.contains(alt)) {
                callers.add("GATK_DE_NOVO");
                break;
            }
        }

        if (p_set.size() == 1 && !Collections.disjoint(alt_set, p_set)) {
            if (m_set.size() == 2 && f_set.size() == 2 && !Collections.disjoint(alt_set, m_set) && !Collections.disjoint(alt_set, f_set)) {
                callers.add("GATK_HOMO_REC");
            }
        }

        if (p_set.size() == 1 && p_set.contains(ref)) {
            if (m_set.size() == 2 && f_set.size() == 2 && m_set.contains(ref) && f_set.contains(ref)) {
                callers.add("GATK_HOMOZYGOUS");
            }
        }

        if (callers.isEmpty()) {
            String inheritance = inherited_from(json, variantContext, samples);
            if ("De-Novo".equals(inheritance)) {
                throw new RuntimeException("Inconsistent inheritance");
            }
            if (!"Inconclusive".equals(inheritance)) {
                callers.add(String.format("INHERITED_FROM: %s", inheritance));
            }
        }

        return callers;
    }

    private static Map<String, Serializable> getCallersData(VariantContext variantContext) {
        if (variantContext == null) {
            return Collections.emptyMap();
        }
        CommonInfo commonInfo = variantContext.getCommonInfo();

        Map<String, Serializable> result = new HashMap<>();
        List<String> callers = getRawCallers(variantContext);
        for (String c : callers) {
            if (commonInfo.hasAttribute(c)) {
                Object value = commonInfo.getAttribute(c);
                if (value instanceof Boolean) {
                    result.put(c, (Boolean) value);
                } else if (value instanceof String) {
                    result.put(c, Long.parseLong((String) value));
                } else {
                    throw new RuntimeException("Not support!");
                }
            }
        }
        return result;
    }

    private static String getZygosity(JSONObject json, VariantContext variantContext, Map<String, Sample> samples) {
        if (samples == null || variantContext == null) {
            return null;
        }
        String chr = RequestParser.toChromosome(getChromosome(json));

        String genotype = (String) getGenotypes(variantContext, samples)[0];
        if (genotype == null) {
            return null;
        }
        List<String> set1 = Arrays.stream(genotype.split("/")).distinct().collect(Collectors.toList());

        if ("X".equals(chr.toUpperCase()) && proband_sex(variantContext, samples) == 1) {
            return "X-linked";
        }
        if (set1.size() == 1) {
            return "Homozygous";
        }
        if (set1.size() == 2) {
            return "Heterozygous";
        }
        return "Unknown";
    }

    private static String inherited_from(JSONObject json, VariantContext variantContext, Map<String, Sample> samples) {
        if (samples == null || variantContext == null) {
            return null;
        }

        Object[] genotypes = getGenotypes(variantContext, samples);
        String probandGenotype = (String) genotypes[0];
        String maternalGenotype = (String) genotypes[1];
        String paternalGenotype = (String) genotypes[2];

        String chr = RequestParser.toChromosome(getChromosome(json));

        if ("X".equals(chr.toUpperCase()) && proband_sex(variantContext, samples) == 1) {
            if (probandGenotype.equals(maternalGenotype)) {
                return "Mother";
            } else {
                return "Inconclusive";
            }
        }

        if (maternalGenotype != null && maternalGenotype.equals(paternalGenotype)) {
            if (probandGenotype.equals(maternalGenotype)) {
                return "Both parents";
            } else {
                return "Both parents";
            }
        }

        if (probandGenotype != null && probandGenotype.equals(maternalGenotype)) {
            return "Mother";
        }
        if (probandGenotype != null && probandGenotype.equals(paternalGenotype)) {
            return "Father";
        }
        return "Inconclusive";
    }

    private static Integer proband_sex(VariantContext variantContext, Map<String, Sample> samples) {
        if (samples == null || variantContext == null) {
            return null;
        }
        String proband = getProband(samples);
        return samples.get(proband).sex;
    }

    private static <T> List<T> unique(List<T> lst) {
        return unique(lst, null);
    }

    /**
     * TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
     * Дело в том, что в python реализации метод основан на set - в итоге проблема с сохранением порядка,
     * непонятно, это сделано специально или нет, если не важна сортировака,
     * то необходимо избавится от этого метода и перейти на Set
     *
     * @param lst
     * @return
     */
    private static <T> List<T> unique(List<T> lst, T replace_None) {
        if (lst == null) {
            return lst;
        }
        List<T> result = new ArrayList<>();
        for (T item : lst) {
            if (!result.contains(item)) {
                result.add(item);
            } else {
                result.remove(item);
                result.add(0, item);
            }
        }
        if (replace_None != null) {
            if (result.contains(null)) {
                result.remove(null);
                if (!result.contains(replace_None)) {
                    result.add(replace_None);
                }
            }
        }
        return result;
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else {
            throw new RuntimeException("Not support type");
        }
    }

    private static double toPrimitiveDouble(Object value) {
        Double p = toDouble(value);
        if (p == null) {
            return 0;
        } else {
            return p;
        }
    }

    @Override
    public void close() {
        anfisaHttpClient.close();
    }
}
