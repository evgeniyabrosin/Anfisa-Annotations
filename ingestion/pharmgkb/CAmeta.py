'''
Create table <name> in db pharmgkb and insert data into it
FILE NAME                    TABLE NAME
clinical_ann_metadata.tsv     CAmeta

COLUMN                      COLUMN NAME         TYPE
Clinical Annotation Id      CAID                INT(10)
Location                    LOC                 TEXT
Gene                        GEN                 TEXT
Level of Evidence           LOE                 VARCHAR(2)
Clinical Annotation Types   CAT                 VARCHAR(50)
Genotype-Phenotypes IDs     -----numbers correspond with the clinical_ann.tsv file.
Annotation Text             AT                  TEXT
Variant Annotations IDs     VAIDS               TEXT
Variant Annotations         VA                  TEXT
PMIDs                       PMIDS               TEXT
Evidence Count              EC                  INT(3)
Related Chemicals           RC                  TEXT
Related Diseases            RD                  TEXT
Race                        RACE                VARCHAR(51)
Chromosome                  CHR                 VARCHAR(5)
'''


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

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS CAmeta(
    CAID                INT(10),
    LOC                 TEXT,
    GEN                 TEXT,
    LOE                 VARCHAR(2),
    CAT                 VARCHAR(50),
    AT                  TEXT,
    VAIDS               TEXT,
    VA                  TEXT,
    PMIDS               TEXT,
    EC                  INT(3),
    RC                  TEXT,
    RD                  TEXT,
    RACE                VARCHAR(51),
    CHR                 VARCHAR(5),
    PRIMARY KEY (CAID));"""

COLUMNS = [
    "CAID",
    "LOC",
    "GEN",
    "LOE",
    "CAT",
    "AT",
    "VAIDS",
    "VA",
    "PMIDS",
    "EC",
    "RC",
    "RD",
    "RACE",
    "CHR",
    ]

INSTR_INSERT = "INSERT INTO CAmeta (%s) VALUES (%s)" % (
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
def ingestCAmeta(db_host, db_port, user, password, database,
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
        try:
            for line in file1:
                row = line.strip('\n').split('\t')
                if row_label == 0:
                    row_label += 1
                    continue
                del row[5]
                list_of_records.append(row)
                if len(list_of_records) >= batch_size:
                    total += execute_insert(conn, INSTR_INSERT,list_of_records)
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
    ingestCAmeta(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "pharmgkb",
        batch_size = 100,
        filename = "/db/data/PharmGKB/annotations/clinical_ann_metadata.tsv")
