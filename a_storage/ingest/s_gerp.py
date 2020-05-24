#===============================================
SCHEMA_GERP = {
    "name": "Gerp",
    "key": "hg19",
    "io": {
        "block-type": "segment",
        "pos-frame": 1000
    },
    "top": {
        "tp": "dict",
        "label": "gerp-rec",
        "items": [
            {"name": "GerpN",  "tp": "num"},
            {"name": "GerpRS",  "tp": "num"}
        ]
    }
}
