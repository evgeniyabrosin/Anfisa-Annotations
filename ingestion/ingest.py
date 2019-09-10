import json, sys, os
from argparse import ArgumentParser

#========================================
parser = ArgumentParser(
    description = "Data ingestion utiities")
parser.add_argument("--mode", "-m",
    help = "Mode: hg19/gerp/spliceai/...")
parser.add_argument("config", nargs = "1")

args = parser.parse_args()

assert os.path.exists(args.config), (
    "Config file not found: " + str(args.config))

with open(args.config, "r", encoding = "utf-8") as inp:
    config = json.loads(inp.read())

#========================================
if args.mode == "hg19":
    from .hg19 import ingestHg19
    ingestHg19(
        db_host    = config["db.host"],
        db_port    = config["db.port"],
        user       = config["db.user"],
        password   = config["db.password"],
        database   = config["hg19.database"],
        fasta_file = config["hg19.fasta_file"])
    sys.exit()

#========================================
if args.mode == "gerp":
    from .gerp import ingestGERP
    ingestGERP(
        db_host    = config["db.host"],
        db_port    = config["db.port"],
        user       = config["db.user"],
        password   = config["db.password"],
        database   = config["gerp.database"],
        batch_size = config["gerp.batch_size"],
        file_list  = config["gerp.file_list"])
    sys.exit()

#========================================
# More modes to add:
# spliceai
# hg38
# clinvar
# gnomad
# ensembl

assert False, "Unsupported ingest mode: " + str(args.mode)

