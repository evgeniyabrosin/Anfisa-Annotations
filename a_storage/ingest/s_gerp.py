#===============================================
SCHEMA_GERP = {
    "name": "Gerp",
    "key": "hg19",
    "io": {
        "block-type": "segment",
        "pos-frame": 400
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
