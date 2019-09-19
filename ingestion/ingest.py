import json, sys, os
from argparse import ArgumentParser

#========================================
parser = ArgumentParser(
    description = "Data ingestion utiities")
parser.add_argument("--mode", "-m",
    help = "Mode: hg19/gerp/spliceai/...")
parser.add_argument("config", nargs = 1)

args = parser.parse_args()

assert os.path.exists(args.config[0]), (
    "Config file not found: " + str(args.config[0]))

with open(args.config[0], "r", encoding = "utf-8") as inp:
    config = json.loads(inp.read())


#========================================
std_db_host    = config["db.host"],
std_db_port    = config["db.port"],
std_user       = config["db.user"],
std_password   = config["db.password"],

#========================================
if args.mode == "hg19":
    from hg19 import ingestHg19
    ingestHg19(
        db_host    = config.get("hg19.db.host", std_db_host),
        db_port    = config.get("hg19.db.port", std_db_port),
        user       = config.get("hg19.db.user", std_user),
        password   = config.get("hg19.db.password", std_password),
        database   = config["hg19.database"],
        fasta_file = config["hg19.fasta_file"])
    sys.exit()

#========================================
if args.mode == "hg38":
    from hg38 import ingestHg38
    ingestHg38(
        db_host    = config.get("hg38.db.host", std_db_host),
        db_port    = config.get("hg38.db.port", std_db_port),
        user       = config.get("hg38.db.user", std_user),
        password   = config.get("hg38.db.password", std_password),
        database   = config["hg38.database"],
        fasta_file = config["hg38.fasta_file"])
    sys.exit()

#========================================
if args.mode == "gerp":
    from gerp import ingestGERP
    ingestGERP(
        db_host    = config.get("gerp.db.host", std_db_host),
        db_port    = config.get("gerp.db.port", std_db_port),
        user       = config.get("gerp.db.user", std_user),
        password   = config.get("gerp.db.password", std_password),
        database   = config["gerp.database"],
        batch_size = config["gerp.batch_size"],
        file_list  = config["gerp.file_list"])
    sys.exit()

#========================================
# More modes to add:
# gnomad
# spliceai
# clinvar
# ensembl

assert False, "Unsupported ingest mode: " + str(args.mode)

