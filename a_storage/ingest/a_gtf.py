import sys, os, gzip, logging, json
from datetime import datetime
from subprocess import Popen, PIPE
from io import TextIOWrapper

from .a_util import (reportTime, extendFileList,
    dumpReader, DirectReader, writeDirect)
#========================================
def c_float(val):
    if val == '.':
        return None
    return float(val)

def c_int(val):
    if val == '.':
        return None
    return float(val)


#========================================
GTF_TAB = [
    ["source",      str,        1],
    ["feature",     str,        2],
    ["start",       int,        3],
    ["end",         int,        4],
    ["score",       c_float,    5],
    ["strand",      str,        6],
    ["frame",       c_int,      7]
    #["attribute",   str,        8]
]

ATTR_IDX = 8

GTF_ATTRS = {
    "gene_name":        ["gene",        str],
    "gene_biotype":     ["biotype",     str],
    "exon_number":      ["exon",        int],
    "transcript_id":    ["transcript",  str]
}

#========================================
def new_record(line, rec_no):
    global GTF_TAB, GTF_ATTRS, ATTR_IDX
    record = {"rec_no": rec_no}
    fields = [val.strip() for val in line.split('\t')]
    chrom = fields[0]
    for key, tp, idx in GTF_TAB:
        record[key] = tp(fields[idx])
    for info in fields[ATTR_IDX].split(';'):
        attr_info = info.strip().split(' ')
        if len(attr_info) > 1:
            akey = attr_info[0].strip()
            if akey in GTF_ATTRS:
                assert len(attr_info) == 2, (
                    "Bad info: " + info + ("/%d" % len(attr_info)))
                key, tp = GTF_ATTRS[akey]
                assert key not in record
                record[key] = tp(attr_info[1].strip().strip('"'))
    return chrom, record

#========================================
def prepareSorted(reader, in_file):
    print("Preparation reading of", in_file)
    with gzip.open(in_file, 'rt', encoding = "utf-8") as text_inp:
        start_time = datetime.now()
        for line_no, line in enumerate(text_inp):
            if line.startswith('#'):
                continue
            chrom, record = new_record(line, reader.sRecNo)
            if reader.checkRecord(chrom):
                if reader.finishSort():
                    yield True
                reader.startSort()
            reader.putRecord(chrom, record)
            if (line_no % 10000) == 0:
                reportTime("Preparation for %s" % chrom, line_no, start_time)
    reader.finishSort()
    reportTime("Done (%s):" % in_file, line_no, start_time)
    reader.sRecNo += 1
    yield True

#========================================
class ReaderGTF:
    sRecNo = 0

    def __init__(self, file_list, tmp_file = "_sort.tmp"):
        self.mFiles = extendFileList(file_list)
        self.mTmpFile = tmp_file
        self.mUsedChrom = set()
        self.mCurChrom = None
        self.mSortProc = None
        self.mSortOutput = None
        logging.info("Using temporary file " + self.mTmpFile)

    def startSort(self):
        assert self.mSortProc is None
        self.mSortProc = Popen("sort -n > " + self.mTmpFile, shell = True,
            stdin = PIPE, stderr = PIPE, bufsize = 1,
            universal_newlines = False, close_fds = True)

        self.mSortOutput = TextIOWrapper(self.mSortProc.stdin,
            encoding = "utf-8", line_buffering = True)

    def finishSort(self):
        if self.mSortProc is None:
            return False
        _, _ = self.mSortProc.communicate()
        self.mSortProc.wait()
        self.mSortOutput.close()
        self.mSortOutput = None
        self.mSortProc = None
        return True

    def checkRecord(self, chrom):
        self.sRecNo += 1
        if self.mCurChrom != chrom:
            assert chrom not in self.mUsedChrom, ("Double chrom %s" % chrom)
            self.mUsedChrom.add(chrom)
            self.mCurChrom = chrom
            return True
        return False

    def putRecord(self, chrom, record):
        print("\t".join([str(record["start"]), str(record["end"]),
            chrom, json.dumps(record, ensure_ascii = False)]),
            file = self.mSortOutput)

    def read(self):
        out_rec_no = 1
        for in_file in self.mFiles:
            for _ in prepareSorted(self, in_file):
                with open(self.mTmpFile, "r", encoding = "utf-8") as inp:
                    rec_list = []
                    diap = None
                    cur_pos = 0
                    for line in inp:
                        start, end, chrom, rec = line.split('\t')
                        start = int(start)
                        end = int(end)
                        if diap is not None and start > diap[1]:
                            if cur_pos < diap[0]:
                                yield ("chr" + chrom, cur_pos), []
                            yield ("chr" + chrom, diap[1]), rec_list
                            rec_list = None
                            cur_pos = diap[1] + 1
                            diap = None
                        if diap is None:
                            diap = [start, end]
                            rec_list = [json.loads(rec)]
                        else:
                            diap[1] = max(diap[1], end)
                            rec_list.append(json.loads(rec))
                        out_rec_no += 1
                    if diap is not None:
                        if cur_pos < diap[0]:
                            yield ("chr" + chrom, cur_pos), []
                        yield ("chr" + chrom, diap[1]), rec_list
                    assert out_rec_no == self.sRecNo
        os.remove(self.mTmpFile)

#========================================
def reader_GTF(properties):
    if "direct_file_list" in properties:
        return DirectReader(properties["direct_file_list"])
    return ReaderGTF(properties["file_list"])


#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    if sys.argv[1] == "DIR":
        reader = reader_GTF({
            "file_list": sys.argv[2]})
        writeDirect(reader, "gtf_dir_%s.js.gz", sys.argv[3])
    else:
        reader = reader_GTF({"file_list": sys.argv[1]})
        dumpReader(reader)
