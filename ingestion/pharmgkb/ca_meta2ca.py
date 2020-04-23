# Create table <name> in db pharmgkb and insert data into it
# FILE NAME                    TABLE NAME
# clinical_ann_metadata.tsv     CAmeta2CA
#
# COLUMN                      COLUMN NAME         TYPE
# Clinical Annotation Id      CAID_CAmeta         INT(10)
# Genotype-Phenotype ID       GPID_CA             INT(10)

import mysql.connector
import time

from util import execute_insert, reportTime
#=== table CAID_CAmeta ============

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS CAmeta2CA (
    CAID_CAmeta         INT(10),
    GPID_CA             INT(10),
    CONSTRAINT meta_to_CA
    PRIMARY KEY(CAID_CAmeta, GPID_CA));"""

COLUMNS = [
    "CAID_CAmeta ",
    "GPID_CA"
    ]

INSTR_INSERT = "INSERT INTO CAmeta2CA(%s) VALUES(%s)" % (
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
def ingestCAmeta2CA(db_host, db_port, user, password, database,
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
                gpids = [int(i) for i in row[5].replace('\"', '').split(',')]
                for i in range(len(gpids)):
                    list_of_records.append([row[0], gpids[i]])
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
    ingestCAmeta2CA(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "pharmgkb",
        batch_size = 100,
        filename = "/db/data/PharmGKB/annotations/clinical_ann_metadata.tsv")
