import json, sys, os
from argparse import ArgumentParser

#========================================
parser = ArgumentParser(
    description = "Data ingestion utiities")
parser.add_argument("--mode", "-m",
    help = "Mode: hg19/hg38/gerp/gnomad/pharmgkb/gtech/...")
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
    from pharmgkb.CA import ingestCA
    from pharmgkb.CAmeta import ingestCAmeta
    from pharmgkb.CAmeta2CA import ingestCAmeta2CA
    from pharmgkb.SPA import ingestSPA
    from pharmgkb.VDA import ingestVDA
    from pharmgkb.VDA2SPA import ingestVDA2SPA
    from pharmgkb.VFA import ingestVFA
    from pharmgkb.VFA2SPA import ingestVFA2SPA
    from pharmgkb.VPA import ingestVPA
    from pharmgkb.VPA2SPA import ingestVPA2SPA
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
# More modes to add:
# gnomad
# spliceai
# clinvar
# ensembl

assert False, "Unsupported ingest mode: " + str(args.mode)

