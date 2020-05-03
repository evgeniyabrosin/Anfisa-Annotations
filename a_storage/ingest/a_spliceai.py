import json, gzip
import vcf as pyvcf
from datetime import datetime

from .a_util import reportTime,  extendFileList
#========================================
VCF_INFO_NAMES = (
    "ALT|SYMBOL|DS_AG|DS_AL|DS_DG|DS_DL|DP_AG|DP_AL|DP_DG|DP_DL".split('|'))

VCF_INFO_TYPES = {
    'DP_AG':    int,
    'DP_AL':    int,
    'DP_DG':    int,
    'DP_DL':    int,
    'DS_AG':    float,
    'DS_AL':    float,
    'DS_DG':    float,
    'DS_DL':    float,
    'ALT':      str,
    'SYMBOL':   str
}

#========================================
def new_single_record(in_record):
    global VCF_INFO_NAMES, VCF_INFO_TYPES

    chrom = in_record.CHROM
    pos = int(in_record.POS)

    record = {key: getattr(in_record, key)
        for key in ("REF", "ID")}

    for fld_name, fld_val in zip(VCF_INFO_NAMES,
            in_record.INFO['SpliceAI'][0].split('|')):
        if fld_name.startswith("#"):
            continue
        record[fld_name] = VCF_INFO_TYPES[fld_name](fld_val)

    record["MAX_DS"] = max(record[key]
        for key in ['DS_AG', 'DS_AL', 'DS_DG', 'DS_DL'])

    return [("chr" + chrom, pos), record]

#===========================================================
class InputDataReader:
    def __init__(self, fname):
        self.mReader = pyvcf.Reader(gzip.open(fname, 'rt'), compressed = False)
        self.mCurRecord = new_single_record(next(self.mReader))

    def isOver(self):
        return self.mCurRecord is None

    def getNext(self):
        if self.mCurRecord is None:
            return None
        key = self.mCurRecord[0]
        seq = [self.mCurRecord[1]]
        while True:
            try:
                in_record = next(self.mReader)
            except Exception:
                self.mCurRecord = None
                return [key, seq]
            self.mCurRecord = new_single_record(in_record)
            if self.mCurRecord[0] == key:
                seq.append(self.mCurRecord[1])
                continue
            assert (key[0] != self.mCurRecord[0][0]
                or key[1] < self.mCurRecord[0][1])
            return [key, seq]

#===========================================================
def processSpliceAI(file_list):
    readers = [InputDataReader(fname) for fname in extendFileList(file_list)]
    buffers = [reader.getNext() for reader in readers]

    start_time = datetime.now()
    total, count = 0, 0
    while True:
        min_key = None
        for idx in range(len(readers)):
            buf = buffers[idx]
            if buf is None:
                buffers[idx] = readers[idx].getNext()
                buf = buffers[idx]
            if buf is not None:
                if min_key is None or min_key > buf[0]:
                    min_key = buf[0]
        if min_key is None:
            break
        res_seq = []
        for idx in range(len(readers)):
            buf = buffers[idx]
            if buf is not None and min_key == buf[0]:
                res_seq += buf[1]
                buffers[idx] = None
        total += len(res_seq)
        count += 1
        if count % 100000 == 0:
            reportTime("", total, start_time)
        yield [min_key, res_seq]
    reportTime("Done", total, start_time)


#========================================
if __name__ == '__main__':
    for key, record in processSpliceAI(
            file_list = "/home/trifon/work/MD/data_ex/spliceai/*.vcf.gz"):
        print(json.dumps({"key": list(key)},
            ensure_ascii = False, sort_keys = True))
        print(json.dumps(record,
            ensure_ascii = False, sort_keys = True))
