package org.forome.annotation.connector.anfisa.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.connector.conservation.struct.Conservation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AnfisaResultView {

    public class Databases {
        public String[] clinVar;

        public Object beacons;
        public String[] pubmedSearch;
        public String[] clinVarVariants;
        public String[] clinVarPhenotypes;
        public String hgmd;
        public String hgmdHg38;
        public String[] hgmdTags;
        public String[] hgmdPhenotypes;
        public String[] hgmdPmids;
        public String[] omim;
        public String[] beaconUrl;
        public String[] clinVarSubmitters;
        public String lmmSignificance;
        public String geneDxSignificance;
        public String[] geneCards;
        public String[] clinVarSignificance;
        public Integer numClinvarSubmitters;
        public String[] clinvarAcmgGuidelines;
        public String clinvarReviewStatus;

        public void setField(String name, String value) {
            switch (name) {
                case "lmm_significance":
                    lmmSignificance = value;
                    break;
                case "gene_dx_significance":
                    geneDxSignificance = value;
                    break;
                default:
                    throw new RuntimeException("not support: " + name);
            }
        }
    }

    public class Predictions {
        public List<String> polyphen2Hvar;
        public List<String> polyphen2Hdiv;
        public List<String> polyphen2HvarScore;
        public List<String> polyphen2HdivScore;
        public List<String> maxEntScan;
        public String[] sift;
        public String[] siftVEP;
        public String[] polyphen;
        public List<Double> revel;
        public String[] siftScore;
        public String[] mutationTaster;
        public String[] fathmm;
        public List<Double> lofScore = new ArrayList<>();
        public List<Double> lofScoreCanonical = new ArrayList<>();
        public List<Double> caddPhred;
        public List<Double> caddRaw;
        public String[] mutationAssessor;
    }

    public static class GnomAD {
        public String allele;
        public Long exomeAn;
        public Double exomeAf;
        public Long genomeAn;
        public Double genomeAf;
        public Double af;
        public String[] url;
        public String proband;
        public List<Double> pli;
        public String popMax;
        public Long hom;
        public Long hem;
    }

    public class General {
        public String probandGenotype;
        public String maternalGenotype;
        public String paternalGenotype;
        public String canonicalAnnotation;
        public String alt;
        public String ref;
        public List<String> cposWorst = new ArrayList<>();
        public List<String> cposCanonical = new ArrayList<>();
        public List<String> cposOther = new ArrayList<>();
        public List<String> spliceRegion;
        public String[] genes;
        public String worstAnnotation;
        public List<String> refseqTranscriptWorst = new ArrayList<>();
        public List<String> ensemblTranscriptsWorst = new ArrayList<>();
        public List<String> refseqTranscriptCanonical = new ArrayList<>();
        public List<String> ensemblTranscriptsCanonical = new ArrayList<>();
        public List<String> geneSplicer;
        public List<String> pposWorst = new ArrayList<>();
        public List<String> pposCanonical = new ArrayList<>();
        public List<String> pposOther = new ArrayList<>();
        public String hg38;
        public String hg19;
        public List<String> variantExonWorst = new ArrayList<>();
        public List<String> variantIntronWorst = new ArrayList<>();
        public List<String> variantExonCanonical = new ArrayList<>();
        public List<String> variantIntronCanonical = new ArrayList<>();
        public Optional<String> spliceAltering;
    }

    public static class Bioinformatics {

        public String zygosity;
        public String inheritedFrom;
        public List<Object> distFromExonWorst = new ArrayList<>();
        public List<Object> distFromExonCanonical = new ArrayList<>();
        public Conservation conservation;
        public String speciesWithVariant;
        public String speciesWithOthers;
        public List<String> maxEntScan;
        public String nnSplice;
        public String humanSplicingFinder;
        public String[] otherGenes;
        public String[] calledBy;
        public Map<String, Serializable> callerData;
        public Map<String, Float> spliceAi;
        public List<String> spliceAiAg = new ArrayList<>();
        public List<String> spliceAiAl = new ArrayList<>();
        public List<String> spliceAiDg = new ArrayList<>();
        public List<String> spliceAiDl = new ArrayList<>();

        public List<String> getSpliceAiValues(String key) {
            switch (key) {
                case "AG":
                    return spliceAiAg;
                case "AL":
                    return spliceAiAl;
                case "DG":
                    return spliceAiDg;
                case "DL":
                    return spliceAiDl;
                default:
                    throw new RuntimeException("Unknown key: " + key);
            }
        }
    }


    public final JSONObject inheritance;
    public final Databases databases;
    public final Predictions predictions;
    public final List<GnomAD> gnomAD;
    public final General general;
    public final Bioinformatics bioinformatics;
    public final JSONArray qualitySamples;

    public AnfisaResultView() {
        this.inheritance = new JSONObject();
        this.databases = new Databases();
        this.predictions = new Predictions();
        this.gnomAD = new ArrayList<>();
        this.general = new General();
        this.bioinformatics = new Bioinformatics();
        this.qualitySamples = new JSONArray();
    }
}