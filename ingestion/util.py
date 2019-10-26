import time, os, re
from glob import glob

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

#=== chromosome detection ================

def detectFileChrom(parameter, filename):
    sChromPatt = re.compile("(\\b|\\W){}(\w+)(\\b|\\W)".format(parameter), re.I)
    CHROM_LIST = {str(idx) for idx in range(1, 23)} | {"M", "X", "Y"}
    qq = sChromPatt.search(os.path.basename(filename))
    assert qq is not None and qq.group(2).upper() in CHROM_LIST, (
        "Failed to detect chrom in filename: " + filename)
    return qq.group(2).upper()

#=== file list extension ================
def extendFileList(files):
   result = []
   for fname in files:
       if '*' in fname:
           result += list(glob(fname))
       else:
           result.append(fname)
   return sorted(result)
