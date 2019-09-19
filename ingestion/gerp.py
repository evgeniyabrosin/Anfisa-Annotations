# INSERTING CHROMS INTO TABLE CONSERVATION , DATABASE conservation

import csv
import mysql.connector
import time

from util import execute_insert, reportTime
from util import detectFileChrom, extendFileList
#========================================
#--- table GERP ----------------

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS GERP(
    Chrom   VARCHAR(4),
    Pos     INT(11),
    GerpN   DOUBLE,
    GerpRS  DOUBLE,
    PRIMARY KEY (Pos, Chrom));"""

COLUMNS = [
    "Chrom",
    "Pos",
    "GerpN",
    "GerpRS"]

INSTR_INSERT = "INSERT INTO GERP (%s) VALUES (%s)" % (
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
def ingestGERP(db_host, db_port, user, password, database,
        batch_size, file_list):

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
    for chrom_file in extendFileList(file_list):
        chrom = detectFileChrom(chrom_file)
        print("Evaluation of", chrom, "in", chrom_file)
        with open (chrom_file,'r') as header:
            position = 0
            list_of_records = []
            total, cnt = 0, 0
            reader = csv.reader (header, delimiter = '\t')
            start_time = time.time()
            for record in reader:
                position += 1
                list_of_records.append(
                    new_record(chrom, position, record))
                if len(list_of_records) >= batch_size:
                    total += execute_insert(conn, INSTR_INSERT,
                        list_of_records)
                    list_of_records = []
                    cnt += 1
                    if cnt >= 100:
                        cnt = 0
                        reportTime("", total, start_time)
            if len(list_of_records) > 0:
                total += execute_insert(conn, INSTR_INSERT, list_of_records)
            reportTime("Done:", total, start_time)

#========================================
if __name__ == '__main__':
    ingestGERP(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "conservation",
        batch_size = 1000,
        file_list = ["/home/trosman/work/Conservations/chr1.maf.rates",])
