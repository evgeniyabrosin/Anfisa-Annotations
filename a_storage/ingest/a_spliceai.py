import sys, logging
import cyvcf2 as pyvcf

from .a_util import (JoinedReader, extendFileList, dumpReader,
    writeDirect, DirectReader)
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
            in_record.INFO['SpliceAI'].split('|')):
        if fld_name.startswith("#"):
            continue
        record[fld_name] = VCF_INFO_TYPES[fld_name](fld_val)

    record["MAX_DS"] = max(record[key]
        for key in ['DS_AG', 'DS_AL', 'DS_DG', 'DS_DL'])

    return [("chr" + chrom, pos), record]

#===========================================================
class InputDataReader:
    def __init__(self, fname):
        self.mReader = pyvcf.VCF(fname)
        self.mCurRecord = new_single_record(next(self.mReader))

    def isOver(self):
        return self.mCurRecord is None

    def close(self):
        self.mReader.close()

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
class ReaderSpliceAI:
    def __init__(self, file_list, max_count = -1):
        readers = [InputDataReader(fname)
            for fname in extendFileList(file_list)]
        self.mJoinedReader = JoinedReader("", readers, max_count)

    def read(self):
        while not self.mJoinedReader.isDone():
            ret = self.mJoinedReader.nextOne()
            if ret is not None:
                yield ret
        self.mJoinedReader.close()

#========================================
def reader_SpliceAI(properties):
    if "direct_file_list" in properties:
        return DirectReader(properties["direct_file_list"])

    return ReaderSpliceAI(
        properties["file_list"],
        properties.get("max_count", -1))


#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    if sys.argv[1] == "DIR":
        reader = reader_SpliceAI({
            "file_list": sys.argv[2]})
        writeDirect(reader, "gnomad_dir_%s.js.gz", sys.argv[4])
    reader = reader_SpliceAI({"file_list": sys.argv[1]})
    dumpReader(reader)
