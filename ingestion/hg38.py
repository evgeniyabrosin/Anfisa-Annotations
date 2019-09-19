import time
import mysql.connector
from pyliftover import LiftOver

from util import execute_insert, reportTime
#========================================
#--- table hg38 ----------------

INSTR_CREATE = """CREATE TABLE IF NOT EXISTS HG38(
    Chrom VARCHAR(2),
    Pos INT(11),
    Ref CHAR(1),
    hg19 INT(11),
    PRIMARY KEY (Pos, Chrom));"""

COLUMNS = [
    "Chrom",
    "Pos",
    "Ref",
    "hg19"]

INSTR_INSERT = "INSERT INTO HG38 (%s) VALUES (%s)" % (
    ", ".join(COLUMNS),
    ", ".join(['%s' for _ in COLUMNS]))

#========================================
#---  fasta reader class ----------------
class Hg38_Reader:
    def __init__(self, fname,
            block_size = 10000,
            chrom_is_int = False,
            upper_case = False):
        self.mInput = open(fname, "r")
        self.mBlockSize  = block_size
        self.mCurLine    = self.mInput.readline()
        self.mChromIsInt = chrom_is_int
        self.mUpperCase  = upper_case
        self.mCurChrom   = None
        self.mCurDiap    = None
        self.mCurLetters = None

    def close(self):
        self.mInput.close()
        self.mInput = None

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

    def read(self):
        if not self.mCurLine:
            self.mCurChrom = -1
            self.mCurLetters = None
            self.mCurDiap = None
            return False
        base_count = 0
        lines = []
        while base_count < self.mBlockSize and self.mCurLine:
            if self.mCurLine.startswith('>'):
                if base_count > 0:
                    break
                assert self.mCurLine.startswith('>chr')
                chrom = self.mCurLine.rstrip()[4:]
                if '_' in chrom:
                    # extra pseudo chromosome information, end of real work
                    self.mCurChrom = -1
                    self.mCurLetters = None
                    self.mCurDiap = None
                    return False
                if self.mChromIsInt:
                    if chrom.isdigit():
                        self.mCurChrom = int(chrom)
                    else:
                        self.mCurChrom = {"M": 0, "X": 23, "Y": 24}.get(chrom)
                else:
                    self.mCurChrom = chrom
                self.mCurDiap = [1, 1]
            elif self.mCurChrom is not None:
                letters = self.mCurLine.rstrip()
                base_count += len(letters)
                if self.mUpperCase:
                    lines.append(letters.upper())
                else:
                    lines.append(letters)
            self.mCurLine = self.mInput.readline()
        if self.mCurChrom is None:
            assert not self.mCurLine
            self.mCurChrom = -1
            self.mCurLetters = None
            self.mCurDiap = None
            return False
        assert all([len(letter_seq) == 50 for letter_seq in lines[:-1]])
        assert len(lines[-1]) == 50 or (len(lines[-1]) < 50 and (
                not self.mCurLine or self.mCurLine.startswith('>')))
        self.mCurLetters = ''.join(lines)
        first_pos = self.mCurDiap[1]
        self.mCurDiap = [first_pos, first_pos + base_count]
        return True

#========================================
#--- liftover class ---------------------

class Converter:
    def __init__(self):
        self.lo = LiftOver('hg38', 'hg19')

    def hg19(self, ch, pos):
        ch = str(ch).upper()
        if (ch.isdigit() or ch == 'X' or ch == 'Y'):
            ch = "chr{}".format(ch)
        try:
            coord  = self.lo.convert_coordinate(ch, pos - 1)
        except:
            print ("WARNING: HG38 conversion at {}:{}".format(ch, pos))
            coord = None
        if (not coord):
            return None
        if (len(coord) == 0):
            return "No Match"
        r = coord[0][1] + 1
        if (len(coord) == 1):
            return r
        return r, coord

    def close(self):
        return

#========================================
def ingestHg38(db_host, db_port, user, password, database, fasta_file):
    conn = mysql.connector.connect(
        host = db_host,
        port = db_port,
        user = user,
        password = password,
        database = database,
        autocommit = True)
    assert conn.is_connected(), "Failed to connect to DB"
    print('Connected to %s...' % database)

    # reader instance
    rd = Hg38_Reader(fasta_file)
    # converter instance
    converter = Converter()

    curs = conn.cursor()
    print (INSTR_CREATE)
    curs.execute(INSTR_CREATE)

    #========================================
    #--- insert values into  hg38 -----------

    total = 0
    start_time = time.time()

    while rd.read():
        list_of_values = []
        for position in range (rd.getCurDiap()[0],rd.getCurDiap()[1]):
            Chrom = rd.getCurChrom()
            r = converter.hg19(Chrom, position)

            values = [Chrom, position, rd.getLetter(position), r]
            list_of_values.append(values)

        total += execute_insert(conn, INSTR_INSERT, list_of_values)
        reportTime("", total, start_time)
        list_of_values = []

    if len(list_of_values) > 0:
        total += execute_insert(conn, INSTR_INSERT, tuple(list_of_values))
    reportTime("Done:", total, start_time)

#========================================
if __name__ == '__main__':
    ingestHg38(
        db_host  = "localhost",
        db_port  = 3306,
        user     = 'test',
        password = 'test',
        database = "util",
        fasta_file = '/home/trosman/work/hg19util/test1000.hg19.fasta')
