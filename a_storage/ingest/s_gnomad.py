#===============================================
VARIANT_ITEMS = [
    {"name": "ALT",     "tp": "str", "opt": "gene"},
    {"name": "REF",     "tp": "str", "opt": "gene"},
    {"name": "SOURCE",  "tp": "str", "opt": "dict"},
    {"name": "AC",      "tp": "num", "format": "%d"},
    {"name": "AN",      "tp": "num", "format": "%d"},
    {"name": "AF",      "tp": "num"},
    {"name": "nhomalt", "tp": "num", "format": "%d"},
    {"name": "faf95",   "tp": "num"},
    {"name": "faf99",   "tp": "num"},
    {
        "tp": "attr-group",
        "group-name": "group",
        "group": ["male", "female", "afr", "amr",
            "asj", "eas", "fin", "nfe", "sas", "oth", "raw"],
        "items": [
            {"name": "AC",  "tp": "num", "format": "%d"},
            {"name": "AN",  "tp": "num", "format": "%d"},
            {"name": "AF",  "tp": "num"}],
    },
    {"name": "hem",  "tp": "num", "format": "%d"}
]

#===============================================
SCHEMA_GNOMAD_2_1 = {
    "name": "GnomAD",
    "key": "hg19",
    "io": {
        "block-type": "cluster",
        "max-var-count": 500
    },
    "filter-list": {"ref": "REF", "alt": "ALT"},
    "top": {
        "tp": "list",
        "item": {
            "tp": "dict",
            "items": VARIANT_ITEMS
        }
    }
}
