# ===================================================
import vcf as pyvcf
import mysql.connector
import time

from util import execute_insert, reportTime
from util import extendFileList, detectFileChrom

#--- table VARIANTS ----------------
# --------------------- creating all columns  ---------------------------
source_fields = ['SOURCE']
main_fields = ['CHROM', 'POS', 'ALT', 'REF']
info_fields = ['nhomalt', 'faf95', 'faf99', 'AC', 'AN', 'AF', 'hem']
prefix_fields = ['AC', 'AN', 'AF']
suffix_fileds = ['afr', 'amr', 'asj', 'eas', 'fin', 'nfe',
    'sas', 'oth', 'raw', 'male', 'female']

columns = {
    'SOURCE':   'varchar(1)',
    'CHROM':    'varchar(4)',
    'POS':      'int(11)',
    'ALT':      'varchar(2048)',
    'REF':      'varchar(512)',
    'AC':       'int(11)',
    'AN':       'int(11)',
    'AF':       'double',
    'hem':       'int(11)',
    'nhomalt':  'int(11)',
    'faf95':    'double',
    'faf99':    'double'
    }
for prefix in prefix_fields:
    for suffix in suffix_fileds:
        # creating info_columns prefix_suffix
        # and adding them to dict info_columns,
        columns[prefix + '_' + suffix] = columns[prefix]
        info_fields.append(prefix + '_' + suffix)
        # their type is the same as in prefix columns(from info_columns).
        # For example:'AC_afr':'int(11)'

fields = source_fields + main_fields + info_fields

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS VARIANTS(
    %s,
    PRIMARY KEY(POS, CHROM, SOURCE, ALT, REF));""" % (
    ", ".join(["%s %s" % (field, columns[field])
    for field in fields]))
#   here we can change PRIMARY KEY

INSTR_INSERT = "INSERT INTO VARIANTS(%s) VALUES(%s)" % (
    ", ".join(fields),
    ", ".join(['%s' for c in fields]))

#===============================================
def new_record(current_file, current_record):
    # Extracts fields in current vcf record to record list
    rec = []
    if 'exomes' in current_file:
        rec.append('e')
    elif 'genomes' in current_file:
        rec.append('g')
    else:
        raise Exception("Genomes or Exomes not specified")
    rec.append(current_record.CHROM)
    rec.append(current_record.POS)
    assert len(current_record.ALT) == 1
    rec.append(str(current_record.ALT[0]))
    rec.append(current_record.REF)
    for field in info_fields:
        try:
            if type(current_record.INFO[field]) == list:
                value = current_record.INFO[field][0]
            else:
                value = current_record.INFO[field]
            if value == 'NA':
                rec.append(None)
            else:
                rec.append(value)
        except KeyError:
            rec.append(None)
    return rec

#===========================================================
def ingestGnomAD(db_host, db_port, user, password,
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
        #!!!! change parameter here !!!!
        chrom = detectFileChrom('exomes.sites.', vcf_file)
        print("Evaluation of", chrom, "in", vcf_file)
        # !!! change compressed to True for .bgz files  !!!
        vcf_reader = pyvcf.Reader(filename = vcf_file, compressed = True)
        start_time = time.time()
        list_of_records = []
        total, cnt = 0, 0
        for record in vcf_reader:
            if len(list_of_records) < batch_size:
                list_of_records.append(new_record(vcf_file, record))
            else:
                total += execute_insert(conn, INSTR_INSERT, list_of_records)
                list_of_records = []
                cnt += 1
                if cnt >= 10:
                    cnt = 0
                    reportTime("Records:", total, start_time)
        if len(list_of_records) > 0:
            total += execute_insert(conn, INSTR_INSERT, list_of_records)
            reportTime("Done:", total, start_time)
            total, cnt = 0, 0
            list_of_records = []
    conn.close()


#========================================
if __name__ == '__main__':
    ingestGnomAD(
        db_host  = 'localhost',
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = 'gnom2',
        batch_size = 1000,
        file_list = ['/home/trosman/work/gnomad211_fields/headers_exomes/'
            + 'head.exomes.sites.*.vcf.bgz'])
