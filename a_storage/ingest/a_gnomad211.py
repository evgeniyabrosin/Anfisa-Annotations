import sys, logging, traceback
from io import StringIO
import cyvcf2 as pyvcf

from .a_util import (detectFileChrom, extendFileList,
    JoinedReader, dumpReader, DirectReader, writeDirect)
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
        self.mReader = pyvcf.VCF(fname)
        self.mCurRecord = self.new_variant(next(self.mReader))
        self.mTotal = 0

    def getFName(self):
        return self.mFName

    def close(self):
        self.mReader.close()

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
            val = getField(info, "nhomalt_male", int)
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
                self.mTotal += 1
            except Exception:
                self.mCurRecord = None
                logging.info("File %s ends with %d records" %
                    (self.mFName, self.mTotal))
                rep = StringIO()
                traceback.print_exc(file = rep)
                logging.info("End exc: " + rep.getvalue())
                return [key, seq]
            self.mCurRecord = self.new_variant(in_record)
            if self.mCurRecord[0] == key:
                seq.append(self.mCurRecord[1])
                continue
            assert (key[0] != self.mCurRecord[0][0]
                or key[1] < self.mCurRecord[0][1])
            return [key, seq]

#===========================================================
class ReaderGNOMAD211:
    def __init__(self, genome_file_list, exome_file_list,
            chrom_loc = "sites.", max_count = -1):
        self.mGenomeFiles = dict()
        for fname in extendFileList(genome_file_list):
            chrom = detectFileChrom(chrom_loc, fname)
            assert chrom not in self.mGenomeFiles
            self.mGenomeFiles[chrom] = fname
        self.mExomeFiles = dict()
        for fname in extendFileList(exome_file_list):
            chrom = detectFileChrom(chrom_loc, fname)
            assert chrom not in self.mExomeFiles
            self.mExomeFiles[chrom] = fname
        self.mChromSeq = sorted(
            set(self.mGenomeFiles.keys()) | set(self.mExomeFiles.keys()),
            reverse = True)
        self.mMaxCount = max_count
        assert len(self.mChromSeq) > 0

    def read(self):
        for chrom in self.mChromSeq:
            readers = []

            fname = self.mGenomeFiles.get(chrom)
            if fname:
                readers.append(InputDataReader(chrom, "g", fname))
            fname = self.mExomeFiles.get(chrom)
            if fname:
                readers.append(InputDataReader(chrom, "e", fname))
            logging.info(("Evaluation of %s chromosome:" % chrom)
                + " ".join([reader.getFName() for reader in readers]))
            join_reader = JoinedReader("chr%s" % chrom,
                readers, self.mMaxCount)
            while not join_reader.isDone():
                ret = join_reader.nextOne()
                if ret is not None:
                    yield ret
            join_reader.close()

#========================================
def reader_GNOMAD211(properties, schema_h = None):
    if "direct_file_list" in properties:
        return DirectReader(properties["direct_file_list"])

    return ReaderGNOMAD211(
        properties["genome_file_list"],
        properties["exome_file_list"],
        properties.get("chrom_loc", "sites."),
        properties.get("max_count", -1))


#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    if sys.argv[1] == "DIR":
        reader = reader_GNOMAD211({
            "genome_file_list": sys.argv[2],
            "exome_file_list": sys.argv[3]})
        writeDirect(reader, "gnomad_dir_%s.js.gz", sys.argv[4])
    else:
        reader = reader_GNOMAD211({
            "genome_file_list": sys.argv[1],
            "exome_file_list": sys.argv[2],
            "max_count": 100})
        dumpReader(reader)
