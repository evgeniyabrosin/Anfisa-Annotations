#===============================================
SCHEMA_GERP = {
    "name": "Gerp",
    "key": "hg19",
    "blocking": {
        "type": "segment",
        "max-pos-count": 400
    },
    "top": {
        "tp": "dict",
        "items": [
            {"name": "GerpN",  "tp": "num"},
            {"name": "GerpRS",  "tp": "num"}
        ]
    }
}
