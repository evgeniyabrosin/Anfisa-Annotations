import json, logging
import vcf as pyvcf
from datetime import datetime

from .a_util import reportTime, detectFileChrom, extendFileList
#========================================
MAIN_FIELDS_TAB = [
    ('AC',      int),
    ('AN',      int),
    ('AF',      float),
    ('nhomalt', int),
    ('faf95',   float),
    ('faf99',   float)
]

GROUPS = ['afr', 'amr', 'asj', 'eas', 'fin', 'nfe',
    'sas', 'oth', 'raw', 'male', 'female']

SUB_FIELDS_TAB = [
    ('AC',      int),
    ('AN',      int),
    ('AF',      float),
]

def getField(info, name, tp):
    val = info.get(name)
    if val is None:
        return None
    if isinstance(val, list):
        assert len(val) == 1
        val = val[0]
    if val == "NA":
        return None
    return tp(val)

#===========================================================
class InputDataReader:
    def __init__(self, chrom, source, fname):
        self.mChrom = chrom
        self.mSource = source
        self.mFName = fname
        self.mReader = pyvcf.Reader(open(fname, 'rb'), compressed = True)
        self.mCurRecord = self.new_variant(next(self.mReader))

    def getFName(self):
        return self.mFName

    def isOver(self):
        return self.mCurRecord is None

    def new_variant(self, in_record):
        global MAIN_FIELDS_TAB, GROUPS, SUB_FIELDS_TAB
        assert in_record.CHROM == self.mChrom
        pos = int(in_record.POS)
        assert len(in_record.ALT) == 1
        record = {
            "SOURCE": self.mSource,
            "REF": str(in_record.REF),
            "ALT": str(in_record.ALT[0])
        }
        info = in_record.INFO
        for name, tp in MAIN_FIELDS_TAB:
            record[name] = getField(info, name, tp)
        for group_name in GROUPS:
            sub_grp = {name: getField(info, name + '_' + group_name, tp)
                for name, tp in SUB_FIELDS_TAB}
            if all(val is None for val in sub_grp.values()):
                continue
            record[group_name] = sub_grp
        if self.mChrom in ("X", "Y"):
            val = getField(info, "nhomalt_male")
            record["hem"] = val if val else 0
        return [("chr" + self.mChrom, pos), record]

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
            self.mCurRecord = self.new_variant(in_record)
            if self.mCurRecord[0] == key:
                seq.append(self.mCurRecord[1])
                continue
            assert (key[0] != self.mCurRecord[0][0]
                or key[1] < self.mCurRecord[0][1])
            return [key, seq]

#===========================================================
def processGNOMAD211(genome_file_list, exome_file_list,
        chrom_loc = "sites.", max_count = -1):
    genome_files = dict()
    for fname in extendFileList(genome_file_list):
        chrom = detectFileChrom(chrom_loc, fname)
        assert chrom not in genome_files
        genome_files[chrom] = fname
    exome_files = dict()
    for fname in extendFileList(exome_file_list):
        chrom = detectFileChrom(chrom_loc, fname)
        assert chrom not in exome_files
        exome_files[chrom] = fname
    chrom_set = set(genome_files.keys()) | set(exome_files.keys())
    assert len(chrom_set) > 0

    for chrom in sorted(chrom_set):
        readers = []

        fname = genome_files.get(chrom)
        if fname:
            readers.append(InputDataReader(chrom, "g", fname))
        fname = exome_files.get(chrom)
        if fname:
            readers.append(InputDataReader(chrom, "e", fname))
        logging.info(("Evaluation of %s chromosome:" % chrom)
            + " ".join([reader.getFName() for reader in readers]))
        buffers = [reader.getNext() for reader in readers]
        start_time = datetime.now()
        total, count = 0, 0
        while True:
            min_key = None
            for idx, buf in enumerate(buffers):
                if buf is None:
                    buf = buffers[idx] = readers[idx].getNext()
                if buf is None:
                    continue
                if min_key is None or min_key > buf[0]:
                    min_key = buf[0]
            if min_key is None:
                break
            res_seq = []
            for idx, buf in enumerate(buffers):
                if buf is not None and min_key == buf[0]:
                    res_seq += buf[1]
                    buffers[idx] = None
            total += len(res_seq)
            count += 1
            if max_count > 0 and count >= max_count:
                break
            if count % 100000 == 0:
                reportTime("Chr" + chrom, total, start_time)
            yield [min_key, res_seq]
        reportTime("Done " + chrom, total, start_time)


#========================================
if __name__ == '__main__':
    for key, record in processGNOMAD211(
            genome_file_list = "/home/trifon/work/MD/data_ex/gnomad/"
            + "gnomad.genomes.r2.1.1.sites.*.vcf.bgz",
            exome_file_list = "/home/trifon/work/MD/data_ex/gnomad/"
            + "gnomad.exomes.r2.1.1.sites.*.vcf.bgz",
            max_count = 100):
        print(json.dumps({"key": list(key)},
            ensure_ascii = False, sort_keys = True))
        print(json.dumps(record,
            ensure_ascii = False, sort_keys = True))
