#===============================================
SCHEMA_SPLICE_AI = {
    "name": "SpliceAI",
    "key": "hg38",
    "io": {
        "block-type": "cluster",
        "max-var-count": 200,
        "str-col-options": {
            "compression": "bz2_compression"
        }
    },
    "filter-list": {"ref": "REF", "alt": "ALT"},
    "top": {
        "tp": "list",
        "item": {
            "tp": "dict",
            "label": "spliceai-rec",
            "items": [
                {"name": "ALT", "tp": "str", "opt": "gene"},
                {"name": "REF", "tp": "str", "opt": "gene"},
                {"name": "ID", "tp": "str", "opt": "gene"},
                {"name": "SYMBOL", "tp": "str", "opt": "repeat"},
                {"name": "DP_AG",  "tp": "num", "format": "%d"},
                {"name": "DP_AL",  "tp": "num", "format": "%d"},
                {"name": "DP_DG",  "tp": "num", "format": "%d"},
                {"name": "DP_DL",  "tp": "num", "format": "%d"},
                {"name": "MAX_DS", "tp": "num"},
                {"name": "DS_AG",  "tp": "num"},
                {"name": "DS_DG",  "tp": "num"},
                {"name": "DS_AL",  "tp": "num"},
                {"name": "DS_DL",  "tp": "num"}
            ]
        }
    }
}
