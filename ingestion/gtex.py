#
# Creates tables GENE, TISSUE, GENE2TISSUE
#       in db gtex and inserts data into them
#

import gzip
from operator import itemgetter
from io import TextIOWrapper
import mysql.connector
import time

from util import execute_insert, reportTime

#=== table GENE ============

INSTR_CREATE_GENE = """CREATE TABLE IF NOT EXISTS GTexGENE(
    GeneName       VARCHAR(24),
    Description    VARCHAR(19),
    TopT1          INT(2),
    TopT2          INT(2),
    TopT3          INT(2),
    PRIMARY KEY(GeneName));"""

COLUMNS_GENE = [
    "GeneName",
    "Description",
    "TopT1",
    "TopT2",
    "TopT3"
    ]

#=== table TISSUE ============
INSTR_CREATE_TISSUE = """CREATE TABLE IF NOT EXISTS GTexTISSUE(
    TissueNo        INT(2),
    Name            VARCHAR(41),
    PRIMARY KEY(TissueNo));"""

COLUMNS_TISSUE = [
    "TissueNo",
    "Name"
    ]

#=== table GENE2TISSUE ============
INSTR_CREATE_GENE2TISSUE = """CREATE TABLE IF NOT EXISTS GTexGENE2TISSUE(
    GeneName         VARCHAR(24),
    TissueNo         INT(2),
    Expression       FLOAT,
    RelExp          FLOAT,
    PRIMARY KEY(GeneName, TissueNo));"""

COLUMNS_GENE2TISSUE = [
    "GeneName",
    "TissueNo",
    "Expression",
    "RelExp"
    ]
#================================================
INSTR_INSERT_GENE = "INSERT INTO GTexGENE (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_GENE),
    ", ".join(['%s' for _ in COLUMNS_GENE]))

INSTR_INSERT_TISSUE = "INSERT INTO GTexTISSUE (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_TISSUE),
    ", ".join(['%s' for _ in COLUMNS_TISSUE]))

INSTR_INSERT_GENE2TISSUE = "INSERT INTO GTexGENE2TISSUE (%s) VALUES (%s)" % (
    ", ".join(COLUMNS_GENE2TISSUE),
    ", ".join(['%s' for _ in COLUMNS_GENE2TISSUE]))
#========================================
def new_record(gen, gname, top1, top2, top3):
    rec = []
    rec.append(gen)
    rec.append(gname)
    rec.append(top1[1])
    for top in [top2, top3]:
        if top[0] == 0:
            rec.append(None)
        else:
            rec.append(top[1])
    return rec

#========================================
def ingestGTEX(db_host, db_port, user, password, database,
        batch_size, filename):

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
    print(INSTR_CREATE_GENE)
    curs.execute(INSTR_CREATE_GENE)
    print(INSTR_CREATE_TISSUE)
    curs.execute(INSTR_CREATE_TISSUE)
    print(INSTR_CREATE_GENE2TISSUE)
    curs.execute(INSTR_CREATE_GENE2TISSUE)

    with gzip.open(filename, 'rb') as inp:
        gene_records, gen2tis_records = [], []
        total_gene, total_gen2tis = 0, 0
        start_time = time.time()
        text_inp = TextIOWrapper(inp,
            encoding = "utf-8", line_buffering = True)
        line_no = 0
        for line in text_inp:
            line_no += 1
            fields = line.split('\t')
            if len(fields) < 3:
                assert line_no < 3
                continue
            if line_no == 3:
                header = [fields[i].strip('\n') for i in range(len(fields))]
                tissues = [(i+1, header[i]) for i in range(2, len(header))]
                c = conn.cursor()
                c.executemany(INSTR_INSERT_TISSUE, tissues)

                print('TISSUE is done')
            if line_no > 3:
                fields[-1] = fields[-1].strip('\n')
                lin = [(-float(fields[i]), i+1)
                    for i in range(2, len(fields))]
                lin.sort(key = itemgetter(0))
                if lin[0][0] == 0:
                    continue
                gene_records.append(new_record(
                    fields[0], fields[1], lin[0], lin[1], lin[2]))
                if len(gene_records) >= batch_size:
                    total_gene += execute_insert(conn,
                        INSTR_INSERT_GENE, gene_records)
                    gene_records = []
                    reportTime("Records_GENE", total_gene, start_time)

                top = lin[0][0]
                for i in range(len(lin)):
                    if lin[i][0] < 0:
                        gen2tis_records.append([fields[0], lin[i][1],
                            -lin[i][0], lin[i][0] / top])
                    else:
                        break
                if len(gen2tis_records) >= batch_size:
                    total_gen2tis += execute_insert(
                        conn, INSTR_INSERT_GENE2TISSUE, gen2tis_records)
                    gen2tis_records = []
                    reportTime("Records_GTexGENE2TISSUE",
                        total_gen2tis, start_time)

        if len(gene_records) >= 0:
            total_gene += execute_insert(conn,
                INSTR_INSERT_GENE, gene_records)
            reportTime("Done_GENE", total_gene, start_time)
        if len(gen2tis_records) >= 0:
            total_gen2tis += execute_insert(conn,
                INSTR_INSERT_GENE2TISSUE, gen2tis_records)
            reportTime("Done_GTexGENE2TISSUE", total_gen2tis, start_time)
    c.close()
    curs.close()


#========================================
if __name__ == '__main__':
    ingestGTEX(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "gtex",
        batch_size = 10000,
        filename = "/db/data/GTEx/GTEx_Analysis_2017-06-05_v8_RNASeQCv1.1.9"
        + "_gene_median_tpm.gct.gz")
