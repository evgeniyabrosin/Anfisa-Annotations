# INSERTING DATA INTO TABLES: FACETS, VARIANTS, TRANSCRIPTS , DATABASE dbnsfp4

import gzip
import mysql.connector
import time
from util import execute_insert, reportTime
from util import detectFileChrom, extendFileList

#========================================
#--- table FACETS ----------------

facets_fields = [1, 2, 3, 4, 30, 31, 41, 72, 73, 74, 79,
    81, 82, 83, 84, 85, 89, 90, 91]
#print(len(facets_fields))
#sys.exit()

INSTR_CREATE_FACETS = """CREATE TABLE IF NOT EXISTS FACETS(
    facet_no                        INT(11),
    chr                             VARCHAR(4),
    pos38                           INT(11),
    ref                             VARCHAR(1),
    alt                             VARCHAR(1),
    refcodon                        TEXT,
    codonpos                        VARCHAR(11),
    SIFT4G_converted_rankscore      FLOAT(7),
    MetaLR_score                    FLOAT(6),
    MetaLR_rankscore                FLOAT(7),
    MetaLR_pred                     VARCHAR(1),
    REVEL_score                     FLOAT(5),
    MutPred_score                   VARCHAR(5),
    MutPred_rankscore               FLOAT(7),
    MutPred_protID                  VARCHAR(15),
    MutPred_AAchange                VARCHAR(7),
    MutPred_Top5features            TEXT,
    MPC_rankscore                   FLOAT(7),
    PrimateAI_score                 FLOAT(14),
    PrimateAI_rankscore             FLOAT(7),
    PRIMARY KEY (pos38, chr, ref, alt, facet_no));"""

COLUMNS_FACETS = [
    "facet_no",
    "chr",
    "pos38",
    "ref",
    "alt",
    "refcodon",
    "codonpos",
    "SIFT4G_converted_rankscore",
    "MetaLR_score",
    "MetaLR_rankscore",
    "MetaLR_pred",
    "REVEL_score",
    "MutPred_score",
    "MutPred_rankscore",
    "MutPred_protID",
    "MutPred_AAchange",
    "MutPred_Top5features",
    "MPC_rankscore",
    "PrimateAI_score",
    "PrimateAI_rankscore"
    ]

INSTR_INSERT_FACETS = "INSERT INTO FACETS (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_FACETS),
    ", ".join(['%s' for _ in COLUMNS_FACETS]))


def new_record_FACETS(records, fac_no):
    rec = []
    rec.append(fac_no)
    for field in records:
        if field == '.':
            rec.append(None)
        else:
            rec.append(field)
    return rec


#========================================
#--- table VARIANTS ----------------

variants_fields = [1, 2, 3, 4, 53, 55, 92,
    102, 104, 105, 106, 114, 115, 116, 117, 118, 119, 374, 375, 376]

INSTR_CREATE_VARIANTS = """CREATE TABLE IF NOT EXISTS VARIANTS(
    chr                             VARCHAR(4),
    pos38                           INT(11),
    ref                             VARCHAR(1),
    alt                             VARCHAR(1),
    MutationTaster_score            TEXT,
    MutationTaster_pred             TEXT,
    PrimateAI_pred                  VARCHAR(1),
    CADD_raw                        FLOAT(9),
    CADD_phred                      FLOAT(5),
    DANN_score                      FLOAT(20),
    DANN_rankscore                  FLOAT(20),
    Eigen_raw_coding                FLOAT(22),
    Eigen_raw_coding_rankscore      FLOAT(7),
    Eigen_pred_coding               FLOAT(12),
    Eigen_PC_raw_coding             FLOAT(21),
    Eigen_PC_raw_coding_rankscore   FLOAT(7),
    Eigen_PC_phred_coding           FLOAT(12),
    GTEx_V7_gene                    TEXT,
    GTEx_V7_tissue                  TEXT,
    Geuvadis_eQTL_target_gene       TEXT,
    PRIMARY KEY (pos38, chr, ref, alt));"""

COLUMNS_VARIANTS = [
    "chr",
    "pos38",
    "ref",
    "alt",
    "MutationTaster_score",
    "MutationTaster_pred",
    "PrimateAI_pred",
    "CADD_raw",
    "CADD_phred",
    "DANN_score",
    "DANN_rankscore",
    "Eigen_raw_coding",
    "Eigen_raw_coding_rankscore",
    "Eigen_pred_coding",
    "Eigen_PC_raw_coding",
    "Eigen_PC_raw_coding_rankscore",
    "Eigen_PC_phred_coding",
    "GTEx_V7_gene",
    "GTEx_V7_tissue",
    "Geuvadis_eQTL_target_gene"
    ]

INSTR_INSERT_VARIANTS = "INSERT INTO VARIANTS (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_VARIANTS),
    ", ".join(['%s' for _ in COLUMNS_VARIANTS]))


def new_record_VARIANTS(records):
    rec = []
    for field in records:
        if field == '.':
            rec.append(None)
        else:
            rec.append(field)
    return rec


#========================================
#--- table TRANSCRIPTS ----------------

transcripts_fields = [1, 14, 15, 16, 17, 19, 20, 21, 22, 26, 37, 39, 40, 42,
    43, 45, 46, 48,  58, 60, 61, 63,  88]


INSTR_CREATE_TRANSCRIPTS = """CREATE TABLE IF NOT EXISTS TRANSCRIPTS(
    facet_no                 INT(11),
    tr_no                    INT(11),
    chr                      VARCHAR(4),
    Ensembl_geneid           VARCHAR(15),
    Ensembl_transcriptid     VARCHAR(15),
    Ensembl_proteinid        VARCHAR(15),
    Uniprot_acc              VARCHAR(30),
    HGVSc_ANNOVAR            TEXT,
    HGVSp_ANNOVAR            TEXT,
    HGVSc_snpEff             TEXT,
    HGVSp_snpEff             TEXT,
    GENCODE_basic            VARCHAR(1),
    SIFT_score               FLOAT(5),
    SIFT_pred                VARCHAR(1),
    SIFT4G_score             FLOAT(5),
    SIFT4G_pred              VARCHAR(1),
    Polyphen2_HDIV_score     FLOAT(5),
    Polyphen2_HDIV_pred      VARCHAR(1),
    Polyphen2_HVAR_score     FLOAT(5),
    Polyphen2_HVAR_pred      VARCHAR(1),
    MutationAssessor_score   FLOAT(19),
    MutationAssessor_pred    VARCHAR(1),
    FATHMM_score             FLOAT(5),
    FATHMM_pred              VARCHAR(1),
    MPC_score                FLOAT(16),
    PRIMARY KEY (facet_no, chr, tr_no));"""

COLUMNS_TRANSCRIPTS = [
    "facet_no",
    "tr_no",
    "chr",
    "Ensembl_geneid",
    "Ensembl_transcriptid",
    "Ensembl_proteinid",
    "Uniprot_acc",
    "HGVSc_ANNOVAR",
    "HGVSp_ANNOVAR",
    "HGVSc_snpEff",
    "HGVSp_snpEff",
    "GENCODE_basic",
    "SIFT_score",
    "SIFT_pred",
    "SIFT4G_score",
    "SIFT4G_pred",
    "Polyphen2_HDIV_score",
    "Polyphen2_HDIV_pred",
    "Polyphen2_HVAR_score",
    "Polyphen2_HVAR_pred",
    "MutationAssessor_score",
    "MutationAssessor_pred",
    "FATHMM_score",
    "FATHMM_pred",
    "MPC_score"
    ]

INSTR_INSERT_TRANSCRIPTS = "INSERT INTO TRANSCRIPTS (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_TRANSCRIPTS),
    ", ".join(['%s' for _ in COLUMNS_TRANSCRIPTS]))


def new_record_TRANSCRIPTS(records, abs_tr_id, current_tr_no):
    rec = []
    rec.append(abs_tr_id)
    rec.append(current_tr_no)
    rec.append(records[0])
    for field in records[1:]:
        if field[current_tr_no] == '.':
            rec.append(None)
        else:
            rec.append(field[current_tr_no])
    return rec

#========================================
class DataCollector:
    def __init__(self, conn, batch_size):
        self.mConn = conn
        self.mBatchSize = batch_size
        self.mFACETS = []
        self.mTRANSCRIPTS = []
        self.mVARIANTS = []
        self.mTotal_FACETS = 0
        self.mTotal_TRANSCRIPTS = 0
        self.mTotal_VARIANTS = 0
        self.mCurVariant = None
        self.mCurFacetNo = 0

    def getTotal_FACETS(self):
        return self.mTotal_FACETS

    def getTotal_TRANSCRIPTS(self):
        return self.mTotal_TRANSCRIPTS

    def getTotal_VARIANTS(self):
        return self.mTotal_VARIANTS

    def flushData(self):
        if len(self.mFACETS) > 0:
            self.mTotal_FACETS += execute_insert(self.mConn,
                INSTR_INSERT_FACETS, self.mFACETS)
            self.mFACETS = []
        if len(self.mTRANSCRIPTS) > 0:
            self.mTotal_TRANSCRIPTS += execute_insert(self.mConn,
                INSTR_INSERT_TRANSCRIPTS, self.mTRANSCRIPTS)
            self.mTRANSCRIPTS = []
        if len(self.mVARIANTS) > 0:
            self.mTotal_VARIANTS += execute_insert(self.mConn,
                INSTR_INSERT_VARIANTS, self.mVARIANTS)
            self.mVARIANTS = []

    def ingestLine(self, line):
        # TRANSCRIPTS
        fields = line.split('\t')
        records = [fields[i].strip('\n') for i in range(len(fields))]
        transcripts_records = [records[i-1] for i in transcripts_fields]
        current_record = [transcripts_records[0]]
        current_record_length = len(transcripts_records[1].split(';'))

        #FACETS
        current_facets_record = [records[i-1] for i in facets_fields]
        self.mFACETS.append(
            new_record_FACETS(current_facets_record, self.mCurFacetNo))
        self.mCurFacetNo += 1

        # VARIANTS
        current_variants_record = [records[i-1] for i in variants_fields]
        if (current_variants_record != self.mCurVariant):
            self.mVARIANTS.append(
                new_record_VARIANTS(current_variants_record))
            self.mCurVariant = current_variants_record

        for i in range(1, (len(transcripts_records))):
            record = transcripts_records[i]
            current_records = record.split(';')
            while (len(current_records) < current_record_length
                    and current_records == ['.']):
                current_records.append(current_records[0])
            current_record.append(current_records)

        for tr_no in range(current_record_length):
            self.mTRANSCRIPTS.append(new_record_TRANSCRIPTS(
                current_record, self.mCurFacetNo, tr_no))
        if len(self.mTRANSCRIPTS) >= self.mBatchSize:
            self.flushData()

#========================================
def ingestDBNSFP4(db_host, db_port, user, password, database,
        batch_size, file_list):
    exceptions = 0
    conn = mysql.connector.connect(
        host = db_host,
        port = db_port,
        user = user,
        password = password,
        database = database,
        autocommit = True)
    assert conn.is_connected(), "Failed to connect to DB"
    print('Connected to %s...' % database)

    curs = conn.cursor()
    print(INSTR_CREATE_FACETS)
    curs.execute(INSTR_CREATE_FACETS)
    print(INSTR_CREATE_TRANSCRIPTS)
    curs.execute(INSTR_CREATE_TRANSCRIPTS)
    print(INSTR_CREATE_VARIANTS)
    curs.execute(INSTR_CREATE_VARIANTS)
    for chrom_file in extendFileList(file_list):
        chrom = detectFileChrom(chrom_file, "chr")  # change parameter here!
        print("Evaluation of", chrom, "in", chrom_file)
        with gzip.open(chrom_file, 'rt') as text_inp:
            start_time = time.time()
            collector = DataCollector(conn, batch_size)
            for line_no, line in enumerate(text_inp):
                if line_no == 0:
                    continue
                try:
                    collector.ingestLine(line)
                    if (line_no % 100) == 0:
                        reportTime("",
                            collector.getTotal_TRANSCRIPTS(), start_time)
                except IndexError:
                    exceptions += 1
            collector.flushData()
            reportTime("Done_TRANSCRIPTS:",
                collector.getTotal_TRANSCRIPTS(), start_time)
            reportTime("Done_VARIANTS:",
                collector.getTotal_VARIANTS(), start_time)
            reportTime("Done_FACETS:",
                collector.getTotal_FACETS(), start_time)
            print('exceptions =', exceptions)


#========================================
if __name__ == '__main__':
    ingestDBNSFP4(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "dbnsfp4",
        batch_size = 1000,
        file_list = [
            "/home/trosman/work/dbNSFP4.0/dbNSFP4.0a_variant.chr*.gz"])
