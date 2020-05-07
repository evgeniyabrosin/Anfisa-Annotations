#===============================================
SCHEMA_SPLICE_AI = {
    "name": "SpliceAI",
    "key": "hg38",
    "io": {
        "block-type": "cluster",
        "max-var-count": 300
    },
    "filter-list": {"ref": "REF", "alt": "ALT"},
    "top": {
        "tp": "list",
        "item": {
            "tp": "dict",
            "items": [
                {"name": "ALT", "tp": "str"},
                {"name": "REF", "tp": "str"},
                {"name": "ID", "tp": "str"},
                {"name": "DP_AG",  "tp": "num", "format": "%d"},
                {"name": "DP_AL",  "tp": "num", "format": "%d"},
                {"name": "DP_DG",  "tp": "num", "format": "%d"},
                {"name": "DP_DL",  "tp": "num", "format": "%d"},
                {"name": "DS_AG",  "tp": "num"},
                {"name": "DS_AL",  "tp": "num"},
                {"name": "DS_DG",  "tp": "num"},
                {"name": "DS_DL",  "tp": "num"},
                {"name": "FAF95",  "tp": "num"},
                {"name": "MAX_DS", "tp": "num"},
                {"name": "SYMBOL", "tp": "str"}
            ]
        }
    }
}
