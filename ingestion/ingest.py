import json, sys, os
from argparse import ArgumentParser

#========================================
parser = ArgumentParser(
    description = "Data ingestion utiities")
parser.add_argument("--mode", "-m",
    help = "Mode: hg19/hg38/gerp/gnomad/pharmgkb/gtech/dbnsfp4/...")
parser.add_argument("config", nargs = 1)

args = parser.parse_args()

assert os.path.exists(args.config[0]), (
    "Config file not found: " + str(args.config[0]))

with open(args.config[0], "r", encoding = "utf-8") as inp:
    config = json.loads(inp.read())

#========================================
std_db_host    = config["db.host"]
std_db_port    = config["db.port"]
std_user       = config["db.user"]
std_password   = config["db.password"]

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
if args.mode == "gnomad":
    from gnomad211 import ingestGnomAD
    ingestGnomAD(
        db_host    = config.get("gnomad.db.host", std_db_host),
        db_port    = config.get("gnomad.db.port", std_db_port),
        user       = config.get("gnomad.db.user", std_user),
        password   = config.get("gnomad.db.password", std_password),
        database   = config["gnomad.database"],
        batch_size = config["gnomad.batch_size"],
        file_list  = config["gnomad.file_list"])
    sys.exit()

#========================================
if args.mode == "pharmgkb":
    from pharmgkb.ca import ingestCA
    from pharmgkb.ca_meta import ingestCAmeta
    from pharmgkb.ca_meta2ca import ingestCAmeta2CA
    from pharmgkb.spa import ingestSPA
    from pharmgkb.vda import ingestVDA
    from pharmgkb.vda2spa import ingestVDA2SPA
    from pharmgkb.vfa import ingestVFA
    from pharmgkb.vfa2spa import ingestVFA2SPA
    from pharmgkb.vpa import ingestVPA
    from pharmgkb.vpa2spa import ingestVPA2SPA
    from pgkb_retab import pgkbReTab

    ingestCA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/clinical_ann.tsv')

    ingestCAmeta(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/clinical_ann_metadata.tsv')

    ingestCAmeta2CA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/clinical_ann_metadata.tsv')

    ingestSPA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/study_parameters.tsv')

    ingestVDA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_drug_ann.tsv')

    ingestVDA2SPA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_drug_ann.tsv')

    ingestVFA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_fa_ann.tsv')

    ingestVFA2SPA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_fa_ann.tsv')

    ingestVPA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_pheno_ann.tsv')

    ingestVPA2SPA(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"],
        batch_size = config["pharmgkb.batch_size"],
        filename   = config["pharmgkb.path"] + '/var_pheno_ann.tsv')

    pgkbReTab(
        db_host    = config.get("pharmgkb.db.host", std_db_host),
        db_port    = config.get("pharmgkb.db.port", std_db_port),
        user       = config.get("pharmgkb.db.user", std_user),
        password   = config.get("pharmgkb.db.password", std_password),
        database   = config["pharmgkb.database"])

    sys.exit()

#========================================
if args.mode == "gtex":
    from gtex import ingestGTEX
    ingestGTEX(
        db_host    = config.get("gtex.db.host", std_db_host),
        db_port    = config.get("gtex.db.port", std_db_port),
        user       = config.get("gtex.db.user", std_user),
        password   = config.get("gtex.db.password", std_password),
        database   = config["gtex.database"],
        batch_size = config["gtex.batch_size"],
        filename  = config["gtex.filename"])
    sys.exit()

#========================================
if args.mode == "spliceai":
    from spliceai import ingestSpliceAI
    ingestSpliceAI(
        db_host    = config.get("spliceai.db.host", std_db_host),
        db_port    = config.get("spliceai.db.port", std_db_port),
        user       = config.get("spliceai.db.user", std_user),
        password   = config.get("spliceai.db.password", std_password),
        database   = config["spliceai.database"],
        batch_size = config["spliceai.batch_size"],
        file_list  = config["spliceai.file_list"])
    sys.exit()

#========================================
if args.mode == "dbnsfp4":
    from dbnsfp4 import ingestDBNSFP4
    ingestDBNSFP4(
        db_host    = config.get("dbnsfp4.db.host", std_db_host),
        db_port    = config.get("dbnsfp4.db.port", std_db_port),
        user       = config.get("dbnsfp4.db.user", std_user),
        password   = config.get("dbnsfp4.db.password", std_password),
        database   = config["dbnsfp4.database"],
        batch_size = config["dbnsfp4.batch_size"],
        file_list  = config["dbnsfp4.file_list"])
    sys.exit()

#========================================
if args.mode == "clinvar":
    from clinvar import ingestCLINVAR
    ingestCLINVAR(
        db_host    = config.get("clinvar.db.host", std_db_host),
        db_port    = config.get("clinvar.db.port", std_db_port),
        user       = config.get("clinvar.db.user", std_user),
        password   = config.get("clinvar.db.password", std_password),
        database   = config["clinvar.database"],
        batch_size = config["clinvar.batch_size"],
        summary_fname  = config["clinvar.variant_summary_file"],
        xml_fname = config["clinvar.XML_FILE"])
    sys.exit()

assert False, "Unsupported ingest mode: " + str(args.mode)
