import time, sys
import mysql.connector
import vcf as pyvcf
from util import execute_insert, reportTime, extendFileList

#=============== table SPLICEAI =========================
DS_list = ['DS_AG', 'DS_AL', 'DS_DG', 'DS_DL']
vcf_columns = {
    "CHROM":    "varchar(4)",
    "POS":      "INT",
    "ID":       "varchar(20)",
    "REF":      "varchar(512)",
    "ALT":      "varchar(2048)"
    }

info_columns = {
    'DP_AG':   "INT",
    'DP_AL':   "INT",
    'DP_DG':   "INT",
    'DP_DL':   "INT",
    'DS_AG':   "FLOAT",
    'DS_AL':   "FLOAT",
    'DS_DG':   "FLOAT",
    'DS_DL':   "FLOAT",
    'SYMBOL':  "varchar(20)",
    'TYPE':    "varchar(1)",
    'STRAND':  "varchar(1)"
    }

max_columns = {
    'MAX_DS':   "FLOAT"
    }
columns = vcf_columns.copy()
columns.update(info_columns)
columns.update(max_columns)
fields = columns.keys()

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS SPLICEAI (
    %s,
    UNIQUE INDEX PosIdx (POS, CHROM, REF, ALT,ID),
    INDEX RsIdIdx (ID));""" % ", ".join(
    ["%s %s" % (field, columns[field]) for field in fields])

INSTR_INSERT = "INSERT INTO SPLICEAI (%s) VALUES (%s)" % (
    ", ".join(fields),
    ", ".join(['%s' for c in fields]))

#============================================================
def new_record(record):
    values = []
    info = record.INFO
    for column in columns:
        if (column in vcf_columns):
            v = getattr(record, column)
            if column == 'ALT':
                if len(v) > 1:
                    print('longALT:', repr(v), file = sys.stderr)
                assert len(v) == 1
                v = str(v[0])
            values.append(v)
        elif column in info_columns:
            v = info[column]
            values.append(v)
        elif column in max_columns:
            v = max(info[key] for key in DS_list)
            values.append(v)
        else:
            raise Exception("%s is %s" % (str(column), str(v)))
    return values
#===========================================================

def ingestSpliceAI(db_host, db_port, user, password,
        database, batch_size, file_list):
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

    for vcf_file in extendFileList(file_list):
        print('vcf_file = %s' % vcf_file)
        vcf_reader = pyvcf.Reader(filename = vcf_file, compressed = True)
        start_time = time.time()
        list_of_records = []
        total, cnt = 0, 0
        for record in vcf_reader:
            try:
                values = new_record(record)
                list_of_records.append(values)
            except Exception:
                raise
            if len(list_of_records) >= batch_size:
                total += execute_insert(conn, INSTR_INSERT, list_of_records)
                list_of_records = []
                cnt += 1
                if cnt >= 10:
                    cnt = 0
                    reportTime("Records:", total, start_time)
        if len(list_of_records) > 0:
            total += execute_insert(conn, INSTR_INSERT, list_of_records)
            reportTime("Done:", total, start_time)


#===========================================================
if __name__ == '__main__':
    ingestSpliceAI(
        db_host  = 'localhost',
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = 'spliceai',
        batch_size = 1000,
        file_list = ['/home/trosman/work/spliceai/*.vcf.gz'])
