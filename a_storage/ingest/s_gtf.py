#===============================================
SCHEMA_GTF = {
    "name": "GTF",
    "key": "hg38",
    "io": {
        "block-type": "frame-idx",
        "pos-keys": ["start", "end"]
    },
    "top": {
        "tp": "list",
        "item": {
            "tp": "dict",
            "items": [
                {"name": "source",      "tp": "str", "opt": "dict"},
                {"name": "feature",     "tp": "str", "opt": "dict"},
                {"name": "start",       "tp": "num", "format": "%d"},
                {"name": "end",         "tp": "num", "format": "%d"},
                {"name": "score",       "tp": "num"},
                {"name": "strand",      "tp": "str", "opt": "dict"},
                {"name": "frame",       "tp": "num", "format": "%d"},
                {"name": "rec_no",      "tp": "num", "format": "%d"},
                {"name": "gene",        "tp": "str", "opt": "repeat"},
                {"name": "biotype",     "tp": "str", "opt": "dict"},
                {"name": "exon",        "tp": "num", "format": "%d"},
                {"name": "transcript",  "tp": "str", "opt": "repeat"}
            ]
        }
    }
}
