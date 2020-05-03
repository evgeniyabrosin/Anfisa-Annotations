import os, re, logging
from glob import glob
from datetime import datetime

#=== timing report ================
def reportTime(note, total, start_time):
    dt = datetime.now() - start_time
    logging.info("%s Records: %d Time: %s; Rate: %.2f"
        % (note, total, str(dt), total / (dt.seconds + .0001)))

#=== chromosome detection ================
def detectFileChrom(parameter, filename):
    chrom_patt = re.compile(r"(\b|\W)%s(\w+)(\b|\W)" % parameter, re.I)
    chrom_list = {str(idx) for idx in range(1, 23)} | {"M", "X", "Y"}
    qq = chrom_patt.search(os.path.basename(filename))
    assert qq is not None and qq.group(2).upper() in chrom_list, (
        "Failed to detect chrom in filename: " + filename)
    return qq.group(2).upper()

#=== file list extension ================
def extendFileList(files):
    if isinstance(files, str):
        files = [files]
    result = []
    for fname in files:
        if '*' in fname:
            result += list(glob(fname))
        else:
            result.append(fname)
    return sorted(result)
