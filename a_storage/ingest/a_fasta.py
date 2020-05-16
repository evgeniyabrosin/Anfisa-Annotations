import gzip
from datetime import datetime

from .a_util import reportTime
#========================================
class FastaReader:
    @staticmethod
    def chromToInt(chrom):
        if chrom.isdigit():
            return int(chrom)
        return {"M": 0, "MT": 0, "X": 23, "Y": 24}.get(chrom)

    def __init__(self, name, fname,
            chrom_is_int = False, to_upper_case = False):
        self.mName = name
        self.mFName = fname
        self.mInput = (gzip.open(self.mFName, "rt")
            if self.mFName.endswith(".gz") else open(self.mFName, "r"))
        self.mCurLine     = self.mInput.readline()
        self.mChromIsInt  = chrom_is_int
        self.mToUpperCase = to_upper_case
        self.mCurChrom    = False
        self.mCurDiap     = None
        self.mCurLetters  = None
        self.mNextLetters = None

    def close(self):
        self.mInput.close()
        self.mInput = None

    def getName(self):
        return self.mName

    def getCurChrom(self):
        return self.mCurChrom

    def getCurDiap(self):
        return self.mCurDiap

    def getCurLetters(self):
        return self.mCurLetters

    def hasPosition(self, pos):
        return self.mCurDiap[0] <= pos < self.mCurDiap[1]

    def getLetter(self, pos):
        assert self.hasPosition(pos)
        return self.mCurLetters[pos - self.mCurDiap[0]]

    def _eof(self):
        assert self.mNextLetters is None
        self.mCurLine = None
        self.mCurChrom = -1 if self.mChromIsInt else None
        self.mCurLetters, self.mCurDiap = None, None

    def readNext(self, block_size = 0x2000):
        if not self.mCurLine:
            self._eof()
            return False
        letter_count = 0
        lines = []
        if self.mNextLetters is not None:
            letter_count += len(self.mNextLetters)
            lines.append(self.mNextLetters)
            self.mNextLetters = None
        while letter_count < block_size and self.mCurLine:
            if self.mCurLine.startswith('>'):
                if letter_count > 0:
                    break
                assert self.mCurLine.startswith('>chr')
                chrom = self.mCurLine.rstrip()[4:]
                if '_' in chrom:
                    # extra chromosome information
                    self.mCurChrom = None
                    self.mCurDiap = None
                else:
                    if self.mChromIsInt:
                        self.mCurChrom = self.chromToInt(chrom)
                    else:
                        self.mCurChrom = chrom
                    self.mCurDiap = [1, 1]
            elif self.mCurDiap is not None:
                letters = self.mCurLine.rstrip()
                letter_count += len(letters)
                if self.mToUpperCase:
                    letters = letters.upper()
                lines.append(letters)
            self.mCurLine = self.mInput.readline()
        if self.mCurChrom is None:
            assert not self.mCurLine and letter_count == 0
            self._eof()
            return False
        assert self.mCurDiap is not None
        self.mCurLetters = ''.join(lines)
        assert len(self.mCurLetters) == letter_count
        if letter_count > block_size:
            self.mNextLetters = self.mCurLetters[block_size:]
            self.mCurLetters = self.mCurLetters[:block_size]
            letter_count = block_size
        first_pos = self.mCurDiap[1]
        self.mCurDiap = [first_pos, first_pos + letter_count]
        return True

    def readAll(self, block_size = 0x2000):
        start_time = datetime.now()
        cnt_blocks = 0
        while self.readNext(block_size):
            yield self.mCurChrom, self.mCurDiap, self.mCurLetters
            cnt_blocks += 1
            if cnt_blocks % 10000 == 0:
                    reportTime("%s At %s:%d"
                        % (self.mFName, self.mCurChrom, self.mCurDiap[0]),
                        cnt_blocks, start_time)
        reportTime("Done %s:" % self.mFName, cnt_blocks, start_time)
        self.close()

#========================================
SCHEMA_FASTA = {
    "name": "fasta",
    "key": "id",
    "types": ["hg19", "hg38"],
    "block-size": 0x2000,
    "io": {
        "block-type": None
    }
}

#========================================
def getFastaSetup(properties):
    global SCHEMA_FASTA
    return SCHEMA_FASTA, [
        FastaReader("hg19", properties["fasta_hg19"]),
        FastaReader("hg38", properties["fasta_hg38"])]
