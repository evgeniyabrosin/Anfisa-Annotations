'''
Create table <name> in db pgkb and insert data into it
FILE NAME                    TABLE NAME
study_parameters.tsv         SPA

COLUMN                           COLUMN NAME         TYPE
Study Parameters ID      	     SPID                INT(10)
Study Type               	     ST                  VARCHAR(56)
Study Cases              	     SC                  VARCHAR(6)
Study Controls             	     SCT                 VARCHAR(6)
Characteristics          	     CH                  TEXT
Characteristics Type     	     CHT                 VARCHAR(12)
Frequency In Cases       	     FIC                 VARCHAR(9)
Allele Of Frequency In Cases     AFCS           	 VARCHAR(57)
Frequency In Controls    	     FICT                VARCHAR(10)
Allele Of Frequency In Controls	 AFCT                VARCHAR(57)
P Value Operator         	     PVO                 VARCHAR(59)
P Value                  	     PV                  VARCHAR(10)
Ratio Stat Type          	     RST                 VARCHAR(7)
Ratio Stat               	     RS                  VARCHAR(8)
Confidence Interval Start	     CSTART              VARCHAR(10)
Confidence Interval Stop 	     CSTOP               VARCHAR(13)
Race(s)                  	     RACE                VARCHAR(61)
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


#=== table SPA ============

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS SPA(
    SPID                INT(10),
    ST                  VARCHAR(56),
    SC                  VARCHAR(6),
    SCT                 VARCHAR(6),
    CH                  TEXT,
    CHT                 VARCHAR(12),
    FIC                 VARCHAR(9),
    AFCS           	VARCHAR(57),
    FICT                VARCHAR(10),
    AFCT                VARCHAR(57),
    PVO                 VARCHAR(59),
    PV                  VARCHAR(10),
    RST                 VARCHAR(7),
    RS                  VARCHAR(8),
    CSTART              VARCHAR(10),
    CSTOP               VARCHAR(13),
    RACE                VARCHAR(61),
    PRIMARY KEY (SPID));"""

COLUMNS = [
    "SPID",
    "ST",
    "SC",
    "SCT",
    "CH",
    "CHT",
    "FIC",
    "AFCS",
    "FICT",
    "AFCT",
    "PVO",
    "PV",
    "RST",
    "RS",
    "CSTART",
    "CSTOP",
    "RACE"
    ]

INSTR_INSERT = "INSERT INTO SPA (%s) VALUES (%s)" % (
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
def ingestSPA(db_host, db_port, user, password, database,
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
                while len(row) > 17:
                    row[4] += '_' + row[5]
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
    ingestSPA(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "pgkb",
        batch_size = 100,
        filename = "/db/data/PharmGKB/annotations/study_parameters.tsv")
