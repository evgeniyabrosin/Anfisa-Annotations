package org.forome.annotation.controller;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.Service;
import org.forome.annotation.annotator.struct.AnnotatorResult;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.anfisa.AnfisaConnector;
import org.forome.annotation.connector.anfisa.struct.AnfisaResult;
import org.forome.annotation.connector.anfisa.struct.AnfisaResultData;
import org.forome.annotation.connector.anfisa.struct.AnfisaResultFilters;
import org.forome.annotation.connector.anfisa.struct.AnfisaResultView;
import org.forome.annotation.connector.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.connector.conservation.struct.Conservation;
import org.forome.annotation.connector.spliceai.struct.SpliceAIResult;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Sample;
import org.forome.annotation.utils.ExecutorServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * http://localhost:8095/GetAnfisaJSON?session=...&data=[{"chromosome": "1", "start": 6484880, "end": 6484880, "alternative": "G"}]
 */
@Controller
@RequestMapping(value = {"/GetAnfisaData", "/annotationservice/GetAnfisaData", "/GetAnfisaJSON", "/annotationservice/GetAnfisaJSON"})
public class GetAnfisaJSONController {

    private final static Logger log = LoggerFactory.getLogger(GetAnfisaJSONController.class);

    public static class RequestItem {

        public final Chromosome chromosome;
        public final long start;
        public final long end;
        public final String alternative;

        public RequestItem(Chromosome chromosome, long start, long end, String alternative) {
            this.chromosome = chromosome;
            this.start = start;
            this.end = end;
            this.alternative = alternative;
        }
    }

    @RequestMapping(value = {"", "/"})
    public CompletableFuture<ResponseEntity> execute(HttpServletRequest request) {
        log.debug("GetAnfisaJSONController execute, time: {}", System.currentTimeMillis());

        Service service = Service.getInstance();

        String sessionId = request.getParameter("session");
        if (sessionId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }
        Long userId = service.getNetworkService().sessionService.checkSession(sessionId);
        if (userId == null) {
            throw ExceptionBuilder.buildInvalidCredentialsException();
        }

        String sRequestData = request.getParameter("data");
        if (Strings.isNullOrEmpty(sRequestData)) {
            throw ExceptionBuilder.buildInvalidValueException("data");
        }

        CompletableFuture<JSONArray> future = new CompletableFuture<>();
        ExecutorServiceUtils.poolExecutor.execute(() -> {
            try {
                long t1 = System.currentTimeMillis();

                ArrayList<RequestItem> requestItems = parseRequestData(sRequestData);

                List<CompletableFuture<AnfisaResult>> futureAnfisaResults = new ArrayList<>();
                AnfisaConnector anfisaConnector = service.getAnfisaConnector();
                for (RequestItem requestItem : requestItems) {
                    futureAnfisaResults.add(anfisaConnector.request(
                            requestItem.chromosome,
                            requestItem.start,
                            requestItem.end,
                            requestItem.alternative
                    ));
                }

                CompletableFuture.allOf(futureAnfisaResults.toArray(new CompletableFuture[futureAnfisaResults.size()]))
                        .thenApply(v -> {
                            JSONArray results = new JSONArray();
                            for (int i = 0; i < requestItems.size(); i++) {
                                RequestItem requestItem = requestItems.get(i);
                                AnfisaResult anfisaResult = futureAnfisaResults.get(i).join();

                                JSONObject result = new JSONObject();
                                result.put("input", new JSONArray() {{
                                    add(requestItem.chromosome);
                                    add(requestItem.start);
                                    add(requestItem.end);
                                    add(requestItem.alternative);
                                }});

                                JSONArray outAnfisaResults = new JSONArray();
                                outAnfisaResults.add(build(anfisaResult));
                                result.put("result", outAnfisaResults);

                                results.add(result);
                            }

                            long t2 = System.currentTimeMillis();
                            log.debug("GetAnfisaJSONController execute request, size: {}, time: {} ms", requestItems.size(), t2 - t1);

                            future.complete(results);
                            return null;
                        })
                        .exceptionally(ex -> {
                            Throwable throwable = ex;
                            if (ex instanceof CompletionException) {
                                throwable = ex.getCause();
                            }
                            log.error("Exception execute request", throwable);
                            future.completeExceptionally(throwable);
                            return null;
                        });
            } catch (Throwable ex) {
                log.error("Exception execute request", ex);
                future.completeExceptionally(ex);
            }
        });

        return future
                .thenApply(out -> {
                    ResponseEntity responseEntity = ResponseBuilder.build(out);
                    log.debug("GetAnfisaJSONController build response, time: {}", System.currentTimeMillis());
                    return responseEntity;

                })
                .exceptionally(throwable -> ResponseBuilder.build(throwable));
    }

    public static ArrayList<RequestItem> parseRequestData(String sRequestData) {
        ArrayList<RequestItem> requestItems = new ArrayList<>();
        JSONArray jRequestData;
        try {
            jRequestData = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(sRequestData);
        } catch (Throwable ex) {
            throw ExceptionBuilder.buildInvalidValueJsonException("data", ex);
        }
        for (Object item : jRequestData) {
            if (!(item instanceof JSONObject)) {
                throw ExceptionBuilder.buildInvalidValueException("data");
            }
            JSONObject oItem = (JSONObject) item;

            Chromosome chromosome = new Chromosome(oItem.getAsString("chromosome"));

            long start = RequestParser.toLong("start", oItem.getAsString("start"));

            long end = RequestParser.toLong("end", oItem.getAsString("end"));

            String alternative = oItem.getAsString("alternative");
            if (Strings.isNullOrEmpty(alternative)) {
                throw ExceptionBuilder.buildInvalidValueException("alternative", alternative, "Description incomplete: please specify alteration (for example, AAC)");
            }
            if (alternative.contains(">")) {//TODO Добавить полноценную валидацию на входной параметр
                throw ExceptionBuilder.buildInvalidValueException("alternative", alternative, "Description incomplete: please specify alteration (for example, AAC)");
            }

            requestItems.add(new RequestItem(
                    chromosome,
                    start,
                    end,
                    alternative
            ));
        }

        return requestItems;
    }

    @VisibleForTesting
    public static JSONObject build(AnfisaResult anfisaResult) {
        JSONObject out = new JSONObject();
        out.put("_filters", buildFilter(anfisaResult.filters, anfisaResult.data, anfisaResult.view.bioinformatics));
        out.put("data", buildData(anfisaResult.data));
        out.put("view", build(anfisaResult.view));
        out.put("record_type", anfisaResult.recordType);
        return out;
    }

    private static JSONObject buildFilter(AnfisaResultFilters anfisaResultFilters,
                                          AnfisaResultData anfisaResultData,
                                          AnfisaResultView.Bioinformatics bioinformatics
    ) {
        JSONObject out = new JSONObject();
        if (anfisaResultFilters.chromosome != null) {
            out.put("chromosome", anfisaResultFilters.chromosome);
        }
        if (anfisaResultFilters.gnomadPopmax != null) {
            out.put("gnomad_popmax", anfisaResultFilters.gnomadPopmax.group.name());
            out.put("gnomad_popmax_af", anfisaResultFilters.gnomadPopmax.af);
            out.put("gnomad_popmax_an", anfisaResultFilters.gnomadPopmax.an);
        }
        if (anfisaResultFilters.gnomadWidePopmax != null) {
            out.put("gnomad_wide_popmax", anfisaResultFilters.gnomadWidePopmax.group.name());
            out.put("gnomad_wide_popmax_af", anfisaResultFilters.gnomadWidePopmax.af);
            out.put("gnomad_wide_popmax_an", anfisaResultFilters.gnomadWidePopmax.an);
        }
        out.put("severity", anfisaResultFilters.severity);
        out.put("gnomad_af_pb", anfisaResultFilters.gnomadAfPb);
        out.put("has_variant", new JSONArray());
        out.put("min_gq", anfisaResultFilters.minGq);
        out.put("dist_from_exon", anfisaResultFilters.distFromExon);
        out.put("proband_gq", anfisaResultFilters.probandGq);
        out.put("fs", anfisaResultFilters.fs);
        out.put("qd", anfisaResultFilters.qd);

        if (anfisaResultFilters.clinvarBenign != null) {
            out.put("clinvar_benign", anfisaResultFilters.clinvarBenign);
        }
        if (anfisaResultFilters.clinvarTrustedBenign != null) {
            out.put("clinvar_trusted_benign", anfisaResultFilters.clinvarTrustedBenign.orElse(null));
        }
        if (anfisaResultFilters.hgmdBenign != null) {
            out.put("hgmd_benign", anfisaResultFilters.hgmdBenign);
        }
        if (anfisaResultFilters.mq != null) {
            out.put("mq", anfisaResultFilters.mq);
        }
        if (anfisaResultFilters.filters != null) {
            out.put("filters", anfisaResultFilters.filters);
        }
        if (anfisaResultFilters.has_variant != null) {
            out.put("has_variant", anfisaResultFilters.has_variant);
        }

        if (anfisaResultFilters.altZygosity != null) {
            out.put("alt_zygosity", anfisaResultFilters.altZygosity);
        }

        out.put("splice_altering", anfisaResultFilters.spliceAltering);
        out.put("splice_ai_dsmax", anfisaResultFilters.spliceAiDsmax);

        out.put("gerp_rs", (bioinformatics.conservation != null) ? bioinformatics.conservation.gerpRS : null);

        out.put("ref", anfisaResultData.ref);
        out.put("alts", anfisaResultFilters.alts);

        out.put("num_clinvar_submitters", anfisaResultFilters.numClinvarSubmitters);

        ClinvarVariantSummary.ReviewStatus clinvarReviewStatus = anfisaResultFilters.clinvarReviewStatus;
        if (clinvarReviewStatus != null) {
            out.put("clinvar_review_status", clinvarReviewStatus.text);
            out.put("clinvar_criteria_provided", (clinvarReviewStatus.getCriteriaProvided() != null) ?
                    clinvarReviewStatus.getCriteriaProvided() : "Unknown");
            out.put("clinvar_conflicts", (clinvarReviewStatus.getConflicts() != null) ?
                    clinvarReviewStatus.getConflicts() : "Unknown");
            out.put("clinvar_stars", clinvarReviewStatus.getStars());
            out.put("clinvar_acmg_guidelines", anfisaResultFilters.clinvarAcmgGuidelines);
        }

        return out;
    }

    private static JSONObject buildData(AnfisaResultData anfisaResultData) {
        JSONObject out = new JSONObject();
        out.put("total_exon_intron_canonical", anfisaResultData.totalExonIntronCanonical);
        out.put("assembly_name", anfisaResultData.assemblyName);
        out.put("start", anfisaResultData.start);
        out.put("end", anfisaResultData.end);
        if (anfisaResultData.alt != null) {
            out.put("alt", anfisaResultData.alt);
        }
        if (anfisaResultData.ref != null) {
            out.put("ref", anfisaResultData.ref);
        }
        if (anfisaResultData.regulatoryFeatureConsequences != null) {
            out.put("regulatory_feature_consequences", anfisaResultData.regulatoryFeatureConsequences);
        }
        if (anfisaResultData.motifFeatureConsequences != null) {
            out.put("motif_feature_consequences", anfisaResultData.motifFeatureConsequences);
        }
        if (anfisaResultData.intergenicConsequences != null) {
            out.put("intergenic_consequences", anfisaResultData.intergenicConsequences);
        }
        out.put("hgmd_pmids", anfisaResultData.hgmdPmids);
        out.put("variant_exon_intron_worst", anfisaResultData.variantExonIntronWorst);
        out.put("variant_exon_intron_canonical", anfisaResultData.variantExonIntronCanonical);
        out.put("id", anfisaResultData.id);
        out.put("allele_string", anfisaResultData.alleleString);
        out.put("seq_region_name", anfisaResultData.seqRegionName);
        out.put("total_exon_intron_worst", anfisaResultData.totalExonIntronWorst);
        out.put("beacon_url", anfisaResultData.beaconUrls);
        if (anfisaResultData.colocatedVariants != null) {
            out.put("colocated_variants", anfisaResultData.colocatedVariants);
        }
        out.put("input", anfisaResultData.input);
        out.put("label", anfisaResultData.label);
        if (anfisaResultData.transcriptConsequences != null) {
            out.put("transcript_consequences", anfisaResultData.transcriptConsequences);
        }
        out.put("most_severe_consequence", anfisaResultData.mostSevereConsequence);
        out.put("strand", anfisaResultData.strand);
        out.put("color_code", (anfisaResultData.colorCode != null) ? anfisaResultData.colorCode.code : null);

        if (anfisaResultData.lmm != null) {
            out.put("lmm", anfisaResultData.lmm);
        }
        if (anfisaResultData.hgmd != null) {
            out.put("HGMD", anfisaResultData.hgmd);
        }
        if (anfisaResultData.hgmdHg38 != null) {
            out.put("HGMD_HG38", anfisaResultData.hgmdHg38);
        }
        if (anfisaResultData.clinVar != null) {
            out.put("ClinVar", anfisaResultData.clinVar);
        }
        if (anfisaResultData.clinvarVariants != null) {
            out.put("clinvar_variants", anfisaResultData.clinvarVariants);
        }
        if (anfisaResultData.clinvarPhenotypes != null) {
            out.put("clinvar_phenotypes", anfisaResultData.clinvarPhenotypes);
        }
        if (anfisaResultData.clinvarSignificance != null) {
            out.put("clinvar_significance", anfisaResultData.clinvarSignificance);
        }
        if (anfisaResultData.clinvarSubmitters != null) {
            out.put("clinvar_submitters", anfisaResultData.clinvarSubmitters);
        }
        if (anfisaResultData.geneDx != null) {
            out.put("gene_dx", anfisaResultData.geneDx);
        }
        if (anfisaResultData.variantClass != null) {
            out.put("variant_class", anfisaResultData.variantClass);
        }
        if (anfisaResultData.distFromBoundaryCanonical != null) {
            out.put("dist_from_boundary_canonical", anfisaResultData.distFromBoundaryCanonical);
        }
        if (anfisaResultData.regionCanonical != null) {
            out.put("region_canonical", anfisaResultData.regionCanonical);
        }
        if (anfisaResultData.distFromBoundaryWorst != null) {
            out.put("dist_from_boundary_worst", anfisaResultData.distFromBoundaryWorst);
        }
        if (anfisaResultData.regionWorst != null) {
            out.put("region_worst", anfisaResultData.regionWorst);
        }
        if (anfisaResultData.zygosity != null) {
            out.put("zygosity", anfisaResultData.zygosity);
        }
        out.put("spliceAI", new JSONObject() {{
            for (Map.Entry<String, SpliceAIResult.DictSql> entry : anfisaResultData.spliceAI.entrySet()) {
                put(
                        entry.getKey(),
                        new JSONObject() {{
                            put("DP_AG", entry.getValue().dp_ag);
                            put("DP_AL", entry.getValue().dp_al);
                            put("DP_DG", entry.getValue().dp_dg);
                            put("DP_DL", entry.getValue().dp_dl);
                            put("DS_AG", entry.getValue().ds_ag);
                            put("DS_AL", entry.getValue().ds_al);
                            put("DS_DG", entry.getValue().ds_dg);
                            put("DS_DL", entry.getValue().ds_dl);
                        }}
                );
            }
        }});
        out.put("version", anfisaResultData.version);
        return out;
    }

    private static JSONObject build(AnfisaResultView anfisaResultView) {
        JSONObject out = new JSONObject();
        out.put("inheritance", anfisaResultView.inheritance);
        out.put("databases", build(anfisaResultView.databases));
        out.put("predictions", build(anfisaResultView.predictions));

        JSONArray jGnomADs = new JSONArray();
        for (AnfisaResultView.GnomAD gnomAD : anfisaResultView.gnomAD) {
            jGnomADs.add(build(gnomAD));
        }
        out.put("gnomAD", jGnomADs);

        out.put("general", build(anfisaResultView.general));
        out.put("bioinformatics", build(anfisaResultView.bioinformatics));
        out.put("quality_samples", anfisaResultView.qualitySamples);
        return out;
    }

    private static JSONObject build(AnfisaResultView.Databases databases) {
        JSONObject out = new JSONObject();
        if (databases.clinVar != null) {
            out.put("clinVar", databases.clinVar);
        }
        out.put("hgmd_pmids", databases.hgmdPmids);
        out.put("beacons", databases.beacons);
        out.put("pubmed_search", databases.pubmedSearch);
        out.put("clinVar_variants", databases.clinVarVariants);
        out.put("clinVar_phenotypes", databases.clinVarPhenotypes);
        out.put("hgmd", databases.hgmd);
        if (databases.hgmdHg38 != null) {
            out.put("hgmd_hg38", databases.hgmdHg38);
        }
        out.put("hgmd_tags", databases.hgmdTags);
        out.put("omim", databases.omim);
        out.put("beacon_url", databases.beaconUrl);
        out.put("gene_dx_significance", databases.geneDxSignificance);
        out.put("hgmd_phenotypes", databases.hgmdPhenotypes);
        out.put("clinVar_submitters", databases.clinVarSubmitters);
        out.put("lmm_significance", databases.lmmSignificance);
        out.put("gene_cards", databases.geneCards);
        out.put("clinVar_significance", databases.clinVarSignificance);
        out.put("num_clinvar_submitters", databases.numClinvarSubmitters);
        out.put("clinvar_acmg_guidelines", databases.clinvarAcmgGuidelines);
        out.put("clinvar_review_status", databases.clinvarReviewStatus);
        return out;
    }

    private static JSONObject build(AnfisaResultView.Predictions predictions) {
        JSONObject out = new JSONObject();
        out.put("polyphen2_hdiv_score", predictions.polyphen2HdivScore);
        out.put("mutation_assessor", predictions.mutationAssessor);
        out.put("fathmm", predictions.fathmm);
        out.put("polyphen2_hdiv", predictions.polyphen2Hdiv);
        out.put("polyphen2_hvar", predictions.polyphen2Hvar);
        out.put("max_ent_scan", predictions.maxEntScan);
        out.put("sift", predictions.sift);
        out.put("sift_vep", predictions.siftVEP);
        out.put("polyphen", predictions.polyphen);
        out.put("revel", predictions.revel);
        out.put("polyphen2_hvar_score", predictions.polyphen2HvarScore);
        out.put("sift_score", predictions.siftScore);
        out.put("cadd_phred", predictions.caddPhred);
        out.put("lof_score_canonical", predictions.lofScoreCanonical);
        out.put("cadd_raw", predictions.caddRaw);
        out.put("mutation_taster", predictions.mutationTaster);
        out.put("lof_score", predictions.lofScore);
        return out;
    }

    private static JSONObject build(AnfisaResultView.GnomAD gnomAD) {
        JSONObject out = new JSONObject();
        if (gnomAD.allele != null) {
            out.put("exome_an", gnomAD.exomeAn);
            out.put("af", gnomAD.af);
            out.put("url", gnomAD.url);
            out.put("exome_af", gnomAD.exomeAf);
            out.put("proband", gnomAD.proband);
            out.put("genome_an", gnomAD.genomeAn);
            out.put("genome_af", gnomAD.genomeAf);
            out.put("allele", gnomAD.allele);
            out.put("pli", gnomAD.pli);
            out.put("wide_popmax", gnomAD.widePopmax);
            out.put("hom", gnomAD.hom);
            out.put("hem", gnomAD.hem);
        } else {
            out.put("url", gnomAD.url);
        }
        return out;
    }

    private static JSONObject build(AnfisaResultView.General general) {
        JSONObject out = new JSONObject();
        out.put("proband_genotype", general.probandGenotype);
        out.put("canonical_annotation", general.canonicalAnnotation);
        out.put("ensembl_transcripts_canonical", general.ensemblTranscriptsCanonical);
        if (general.alt != null) {
            out.put("alt", general.alt);
        }
        if (general.ref != null) {
            out.put("ref", general.ref);
        }
        out.put("ppos_canonical", general.pposCanonical);
        out.put("cpos_worst", general.cposWorst);
        out.put("variant_intron_canonical", general.variantIntronCanonical);
        out.put("cpos_other", general.cposOther);
        out.put("splice_region", general.spliceRegion);
        out.put("maternal_genotype", general.maternalGenotype);
        out.put("variant_intron_worst", general.variantIntronWorst);
        out.put("paternal_genotype", general.paternalGenotype);
        out.put("genes", general.genes);
        out.put("variant_exon_worst", general.variantExonWorst);
        out.put("worst_annotation", general.worstAnnotation);
        out.put("cpos_canonical", general.cposCanonical);
        out.put("variant_exon_canonical", general.variantExonCanonical);
        out.put("refseq_transcript_worst", general.refseqTranscriptWorst);
        out.put("refseq_transcript_canonical", general.refseqTranscriptCanonical);
        out.put("gene_splicer", general.geneSplicer);
        out.put("ppos_worst", general.pposWorst);
        out.put("ppos_other", general.pposOther);
        out.put("hg38", general.hg38);
        out.put("hg19", general.hg19);
        out.put("ensembl_transcripts_worst", general.ensemblTranscriptsWorst);
        if (general.spliceAltering != null) {
            out.put("splice_altering", general.spliceAltering.orElse(null));
        }
        return out;
    }

    private static JSONObject build(AnfisaResultView.Bioinformatics bioinformatics) {
        JSONObject out = new JSONObject();
        out.put("human_splicing_finder", bioinformatics.humanSplicingFinder);
        out.put("zygosity", bioinformatics.zygosity);
        out.put("dist_from_exon_worst", bioinformatics.distFromExonWorst);
        out.put("called_by", bioinformatics.calledBy);
        out.put("max_ent_scan", bioinformatics.maxEntScan);
        out.put("dist_from_exon_canonical", bioinformatics.distFromExonCanonical);
        out.put("conservation", build(bioinformatics.conservation));
        out.put("caller_data", bioinformatics.callerData);
        out.put("nn_splice", bioinformatics.nnSplice);
        out.put("species_with_variant", bioinformatics.speciesWithVariant);
        out.put("other_genes", bioinformatics.otherGenes);
        out.put("species_with_others", bioinformatics.speciesWithOthers);
        out.put("inherited_from", bioinformatics.inheritedFrom);
        out.put("splice_ai", bioinformatics.spliceAi);
        out.put("splice_ai_ag", bioinformatics.spliceAiAg);
        out.put("splice_ai_al", bioinformatics.spliceAiAl);
        out.put("splice_ai_dg", bioinformatics.spliceAiDg);
        out.put("splice_ai_dl", bioinformatics.spliceAiDl);
        out.put("gerp_rs", (bioinformatics.conservation != null) ? bioinformatics.conservation.gerpRS : null);
        return out;
    }

    private static JSONObject build(Conservation conservation) {
        if (conservation == null) {
            return null;
        }
        JSONObject out = new JSONObject();
        out.put("pri_ph_cons", conservation.priPhCons);
        out.put("mam_ph_cons", conservation.mamPhCons);
        out.put("ver_ph_cons", conservation.verPhCons);
        out.put("pri_phylo_p", conservation.priPhyloP);
        out.put("mam_phylo_p", conservation.mamPhyloP);
        out.put("ver_phylo_p", conservation.verPhyloP);
        out.put("gerp_r_s", conservation.gerpRS);
        out.put("gerp_r_spval", conservation.getGerpRSpval());
        out.put("gerp_n", conservation.gerpN);
        out.put("gerp_s", conservation.gerpS);
        return out;
    }

    public static JSONObject build(AnnotatorResult.Metadata metadata) {
        JSONObject out = new JSONObject();
        out.put("case", metadata.caseSequence);
        out.put("record_type", metadata.recordType);
        out.put("versions", build(metadata.versions));
        out.put("samples", new JSONObject() {{
            for (Sample sample : metadata.samples.values()) {
                put(sample.name, build(sample));
            }
        }});
        return out;
    }

    public static JSONObject build(AnnotatorResult.Metadata.Versions versions) {
        JSONObject out = new JSONObject();
        out.put("pipeline_date", versions.pipelineDate);
        out.put("annotations_date", versions.annotationsDate);
        out.put("pipeline", versions.pipeline);
        out.put("annotations", versions.annotations);
        out.put("reference", versions.reference);
        for (DatabaseConnector.Metadata metadata : versions.metadataDatabases) {
            StringBuilder value = new StringBuilder();
            if (metadata.version != null) {
                value.append(metadata.version);
                if (metadata.date != null) {
                    value.append(" | ");
                }
            }
            if (metadata.date != null) {
                value.append(DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.systemDefault()).format(metadata.date));
            }
            out.put(metadata.product, value.toString());
        }
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
