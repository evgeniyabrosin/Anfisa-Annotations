# Create table <name> in db pharmgkb and insert data into it
# FILE NAME                    TABLE NAME
# var_drug_ann.tsv             VDA
#
# COLUMN                      COLUMN NAME         TYPE
# Annotation ID            	 AID                INT(10)
# Variant                  	 VAR                TEXT
# Gene                     	 GENE               TEXT
# Chemical                 	 CHEM               TEXT
# PMID                     	 PMID               INT(10)
# Phenotype Category       	 PCAT               VARCHAR(46)
# Significance             	 SIGN               VARCHAR(10)
# Notes                    	 NOTES              TEXT
# Sentence                 	 SENT               TEXT
# StudyParameters          	 corresponds with the Study Parameters ID
#                               in the study_parameters.tsv file
# Alleles                  	 AL                 TEXT
# Chromosome               	 CHROM              VARCHAR(5)

import mysql.connector
import time

from util import execute_insert, reportTime
#=== table VDA ============

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS PharmVDA (
    AID                INT(10),
    VAR                TEXT,
    GENE               TEXT,
    CHEM               TEXT,
    PMID               INT(10),
    PCAT               VARCHAR(46),
    SIGN               VARCHAR(10),
    NOTES              BLOB,
    SENT               TEXT,
    AL                 TEXT,
    CHROM              VARCHAR(5),
    PRIMARY KEY(AID));"""

COLUMNS = [
    "AID",
    "VAR",
    "GENE",
    "CHEM",
    "PMID",
    "PCAT",
    "SIGN",
    "NOTES",
    "SENT",
    "AL",
    "CHROM"
    ]

INSTR_INSERT = "INSERT INTO PharmVDA(%s) VALUES(%s)" % (
    ", ".join(COLUMNS),
    ", ".join(['%s' for _ in COLUMNS]))

#========================================
def new_record(chrom, pos, lst):
    rec = []
    rec.append(chrom)
    rec.append(pos)
    for item in lst:
        if item == 'NA':
            rec.append(None)
        else:
            rec.append(item)
    return rec

#========================================
def ingestVDA(db_host, db_port, user, password, database,
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
    print(INSTR_CREATE)
    curs.execute(INSTR_CREATE)

    with open(filename, 'r') as file1:
        list_of_records = []
        total, cnt, row_label = 0, 0, 0
        start_time = time.time()
        try:
            for line in file1:
                row = line.strip('\n').split('\t')
                if row_label == 0:
                    row_label += 1
                    continue
                del row[9]
                list_of_records.append(row)
                if len(list_of_records) >= batch_size:
                    total += execute_insert(
                        conn, INSTR_INSERT, list_of_records)
                    list_of_records = []
                    cnt += 1
                    if cnt >= 10:
                        cnt = 0
                        reportTime("Records", total, start_time)
        except mysql.connector.errors.DataError:
            print(len(row))
            print(row)
        if len(list_of_records) > 0:
            total += execute_insert(conn, INSTR_INSERT, list_of_records)
        reportTime("Done:", total, start_time)


#========================================
if __name__ == '__main__':
    ingestVDA(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "pharmgkb",
        batch_size = 100,
        filename = "/db/data/PharmGKB/annotations/var_drug_ann.tsv")
