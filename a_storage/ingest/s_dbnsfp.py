#===============================================
_TRASCRIPT_PROPERTIES = [
    {"name": "Ensembl_geneid",          "tp": "str"},
    {"name": "Ensembl_transcriptid",    "tp": "str"},
    {"name": "Ensembl_proteinid",       "tp": "str"},
    {"name": "FATHMM_score",            "tp": "num"},
    {"name": "FATHMM_pred",             "tp": "str", "opt": "dict"},
    {"name": "GENCODE_basic",           "tp": "str"},
    {"name": "HGVSc_ANNOVAR",           "tp": "str"},
    {"name": "HGVSp_ANNOVAR",           "tp": "str"},
    {"name": "HGVSc_snpEff",            "tp": "str"},
    {"name": "HGVSp_snpEff",            "tp": "str"},
    {"name": "MPC_score",               "tp": "num"},
    {"name": "MutationTaster_score",    "tp": "num"},
    {"name": "MutationAssessor_pred",   "tp": "str", "opt": "dict"},
    {"name": "Polyphen2_HDIV_score",    "tp": "num"},
    {"name": "Polyphen2_HDIV_pred",     "tp": "str", "opt": "dict"},
    {"name": "Polyphen2_HVAR_score",    "tp": "num"},
    {"name": "Polyphen2_HVAR_pred",     "tp": "str", "opt": "dict"},
    {"name": "SIFT_score",              "tp": "num"},
    {"name": "SIFT_pred",               "tp": "str", "opt": "dict"},
    {"name": "SIFT4G_score",            "tp": "num"},
    {"name": "SIFT4G_pred",             "tp": "str", "opt": "dict"},
    {"name": "Uniprot_acc",              "tp": "str"}
]

#===============================================
_FACETS_PROPERTIES = [
    {"name": "refcodon",                    "tp": "str"},
    {"name": "codonpos",                    "tp": "str"},
    {"name": "MetaLR_score",                "tp": "num"},
    {"name": "MetaLR_rankscore",            "tp": "num"},
    {"name": "MetaLR_pred", "opt": "dict",  "tp": "str"},
    {"name": "MutPred_score",               "tp": "str"},
    {"name": "MutPred_rankscore",           "tp": "num"},
    {"name": "MutPred_protID",              "tp": "str"},
    {"name": "MutPred_AAchange",            "tp": "str"},
    {"name": "MutPred_Top5features",        "tp": "str"},
    {"name": "MPC_rankscore",               "tp": "num"},
    {"name": "PrimateAI_score",             "tp": "num"},
    {"name": "PrimateAI_rankscore",         "tp": "num"},
    {"name": "REVEL_score",                 "tp": "num"},
    {"name": "SIFT4G_converted_rankscore",  "tp": "num"},
    {
        "name": "transcripts", "tp": "list",
        "item": {
            "tp": "dict", "items": _TRASCRIPT_PROPERTIES
        }
    }
]

#===============================================
_VARIANT_PROPERTIES = [
    {"name": "ALT", "tp": "str"},
    {"name": "REF", "tp": "str"},
    {"name": "CADD_raw",                        "tp": "num"},
    {"name": "CADD_phred",                      "tp": "num"},
    {"name": "DANN_score",                      "tp": "num"},
    {"name": "DANN_rankscore",                  "tp": "num"},
    {"name": "Eigen_raw_coding",                "tp": "num"},
    {"name": "Eigen_raw_coding_rankscore",      "tp": "num"},
    {"name": "Eigen_phred_coding",              "tp": "num"},
    {"name": "Eigen_PC_raw_coding",             "tp": "num"},
    {"name": "Eigen_PC_raw_coding_rankscore",   "tp": "num"},
    {"name": "Eigen_PC_phred_coding",           "tp": "num"},
    {"name": "GTEx_V7_gene",                "tp": "str"},
    {"name": "GTEx_V7_tissue",              "tp": "str", "opt": "dict"},
    {"name": "MutationTaster_score",        "tp": "str"},
    {"name": "MutationTaster_pred",         "tp": "str"},
    {"name": "PrimateAI_pred",              "tp": "str", "opt": "dict"},
    {"name": "Geuvadis_eQTL_target_gene",   "tp": "str"},
    {
        "name": "facets",
        "tp": "list",
        "item": {
            "tp": "dict",
            "items": _FACETS_PROPERTIES
        }
    }
]

#===============================================
SCHEMA_DBNSFP_4 = {
    "name": "DBNSFP",
    "key": "hg38",
    "io": {
        "block-type": "cluster",
        "max-var-count": 50
    },
    "filter-list": {"ref": "REF", "alt": "ALT"},
    "top": {
        "tp": "list",
        "item": {
            "tp": "dict",
            "items": _VARIANT_PROPERTIES
        }
    }
}
