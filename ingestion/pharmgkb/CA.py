'''
Create table <name> in db pharmgkb and insert data into it
FILE NAME               TABLE NAME
clinical_ann.tsv        CA

COLUMN                   COLUMN NAME         TYPE
Genotype-Phenotype ID    GPID                INT(10)
Genotype                 GTYPE               TEXT
Clinical Phenotype       CPTYPE              TEXT

'''

import csv
import mysql.connector
import time

#=== execute insert function ================
def execute_insert(conn, sql, list_of_values):
    rowcount = 0
    c = conn.cursor()
    if (len(list_of_values) == 1):
        c.execute(sql, list_of_values[0])
        rowcount += c.rowcount
    else:
        c.executemany(sql, list_of_values)
        rowcount += c.rowcount
    c.close()
    return rowcount

#=== timing report ================
def reportTime(note, total, start_time):
    dt = time.time() - start_time
    print ("{} Records: {} Time: {}; Rate: {:.2f}".format(
        note, total, dt, total / (dt + .0001)))


#=== table CA ============

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS CA(
    GPID                INT(10),
    GTYPE               TEXT,
    CPTYPE              TEXT,
    PRIMARY KEY (GPID));"""

COLUMNS = [
    "GPID",
    "GTYPE",
    "CPTYPE"]

INSTR_INSERT = "INSERT INTO CA (%s) VALUES (%s)" % (
    ", ".join(COLUMNS),
    ", ".join(['%s' for _ in COLUMNS]))

#========================================
def new_record (chrom, pos, lst):
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
def ingestCA(db_host, db_port, user, password, database,
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
    print (INSTR_CREATE)
    curs.execute(INSTR_CREATE)

    with open (filename,'r') as file1:
        list_of_records = []
        total, cnt, row_label = 0, 0, 0
        start_time = time.time()
        for line in file1:
            row = line.strip('\n').split('\t')
            if row_label == 0:
               row_label += 1
               continue

            list_of_records.append(row)
            if len(list_of_records) >= batch_size:
                    total += execute_insert(conn, INSTR_INSERT,
                        list_of_records)
                    list_of_records = []
                    cnt += 1
                    if cnt >= 100:
                        cnt = 0
                        reportTime("Records", total, start_time)
        if len(list_of_records) > 0:
            total += execute_insert(conn, INSTR_INSERT, list_of_records)
        reportTime("Done:", total, start_time)


#========================================
if __name__ == '__main__':
    ingestCA(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "pharmgkb",
        batch_size = 100,
        filename = "/db/data/PharmGKB/annotations/clinical_ann.tsv")
