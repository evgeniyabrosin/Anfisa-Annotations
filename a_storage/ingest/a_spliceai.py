import sys, logging, gzip
import cyvcf2 as pyvcf
from datetime import datetime

from .a_util import (JoinedReader, extendFileList, dumpReader,
    writeDirect, DirectReader, detectFileChrom, reportTime)
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
    def __init__(self, chrom, fname):
        self.mFName = fname
        self.mReader = pyvcf.VCF(fname)
        self.mCurRecord = False
        self.mChrom = chrom
        if not self.mChrom.startswith("chr"):
            self.mChrom = "chr" + self.mChrom

    def getFName(self):
        return self.mFName

    def isOver(self):
        return self.mCurRecord is None

    def close(self):
        self.mReader.close()

    def getNext(self):
        if self.mCurRecord is None:
            return None, None
        if self.mCurRecord is False:
            key, seq = None, None
        else:
            key, seq = self.mCurRecord[0], [self.mCurRecord[1]]
        while True:
            try:
                in_record = next(self.mReader)
            except Exception:
                self.mCurRecord = None
                return [key, seq]
            self.mCurRecord = new_single_record(in_record)
            if key is None:
                key, seq = self.mCurRecord[0], [self.mCurRecord[1]]
                continue
            if self.mCurRecord[0] == key:
                seq.append(self.mCurRecord[1])
                continue
            assert key is None or (key[0] != self.mCurRecord[0][0]
                or key[1] < self.mCurRecord[0][1])
            assert key[0] == self.mChrom, (
                "Chrom conflict: %s/%s" % (key[0], self.mChrom))
            return [key, seq]

#===========================================================
class ReaderSpliceAI:
    def __init__(self, indel_file_list, snv_file_list,
            chrom_loc = "spliceai.chr"):
        self.mIndelFiles = dict()
        for fname in extendFileList(indel_file_list):
            chrom = detectFileChrom(chrom_loc, fname)
            assert chrom not in self.mIndelFiles
            self.mIndelFiles[chrom] = fname
        self.mSnvFiles = dict()
        for fname in extendFileList(snv_file_list):
            chrom = detectFileChrom(chrom_loc, fname)
            assert chrom not in self.mSnvFiles
            self.mSnvFiles[chrom] = fname
        self.mChromSeq = sorted(
            set(self.mIndelFiles.keys()) | set(self.mSnvFiles.keys()),
            reverse = True)
        assert len(self.mChromSeq) > 0

    def read(self):
        for chrom in self.mChromSeq:
            readers = []
            fname = self.mIndelFiles.get(chrom)
            if fname:
                readers.append(InputDataReader(chrom, fname))
            fname = self.mIndelFiles.get(chrom)
            if fname:
                readers.append(InputDataReader(chrom, fname))
            logging.info(("Evaluation of %s chromosome:" % chrom)
                + " ".join([reader.getFName() for reader in readers]))
            join_reader = JoinedReader("chr%s" % chrom, readers)
            while not join_reader.isDone():
                ret = join_reader.nextOne()
                if ret is not None:
                    yield ret
            join_reader.close()

#========================================
def reader_SpliceAI(properties):
    if "direct_file_list" in properties:
        return DirectReader(properties["direct_file_list"])

    return ReaderSpliceAI(
        properties["indel_file_list"],
        properties["snv_file_list"])

#========================================
def splitPreparation(in_long_file, out_dir):
    cur_chrom = None
    cur_fname = None
    output = None
    out_count = None
    used_chroms = set()
    headers = []
    start_time = datetime.now()

    if "indel" in in_long_file:
        mode = "indel"
    elif "snv" in in_long_file:
        mode = "snv"
    else:
        assert False, "Mode (indel/snv) not found"
    file_pattern = out_dir + "/" + mode + ".spliceai.chr%s.vcf.gz"

    with gzip.open(in_long_file, "rt") as inp:
        for line_no, line in enumerate(inp):
            if line.startswith('#'):
                headers.append(line)
                continue
            chrom = line[:line.index('\t')]
            if chrom != cur_chrom:
                if output is not None:
                    output.close()
                    logging.info("Done %s: %d records"
                        % (cur_fname, out_count))
                    output = None
                    out_count = None
                cur_chrom = chrom
                if '_' not in cur_chrom:
                    assert cur_chrom not in used_chroms
                    used_chroms.add(cur_chrom)
                    cur_fname = file_pattern % cur_chrom
                    logging.info("Open %s" % cur_fname)
                    output = gzip.open(cur_fname, "wt")
                    for hline in headers:
                        output.write(hline)
                    out_count = 0
            if output is not None:
                output.write(line)
                out_count += 1
            if (line_no % 100000) == 0:
                pos = line.split('\t')[1]
                reportTime("At %s:%s:" % (cur_chrom, pos), line_no, start_time)
        if output is not None:
            output.close()
            logging.info("Done %s: %d records"
                % (cur_fname, out_count))

#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    if sys.argv[1] == "SPLIT":
        splitPreparation(sys.argv[2], sys.argv[3])
    elif sys.argv[1] == "DIR":
        logging.info("Using direct preparation in " + sys.argv[4])
        reader = reader_SpliceAI({
            "indel_file_list": sys.argv[2],
            "snv_file_list": sys.argv[3]})
        writeDirect(reader, "spliceai_dir_%s.js.gz", sys.argv[4])
    else:
        reader = reader_SpliceAI({
            "indel_file_list": sys.argv[1],
            "snv_file_list": sys.argv[2]})
        dumpReader(reader)
