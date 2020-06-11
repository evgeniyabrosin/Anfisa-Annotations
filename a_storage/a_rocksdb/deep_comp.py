import array, json
#========================================
DEEP_PREXIX = b"AStorage/DeepComp-0.2\n"
DEEP_PREXIX_PRE = b"AStorage/DeepComp-0.1\n"

class DeepCompWriter:
    def __init__(self, fname):
        global DEEP_PREXIX
        self.mFName = fname
        self.mOutput = open(fname, "wb")
        self.mOutput.write(DEEP_PREXIX)
        self.mColumn = None

    def put(self, xkey, column_h, xdata):
        c_name = column_h.getName()
        if self.mColumns is None:
            self.mColumn = c_name
            rep_columns = json.dumps([self.mColumn.decode(encoding = "utf-8")],
                ensure_ascii = False)
            self.mOutput.write(bytes(rep_columns + '\n', encoding = "utf-8"))
        else:
            assert self.mColumn == c_name, (
                "Only one column is supported: %s/%s" %(self.mColumn, c_name))
        header = array.array('L')
        header = [len(xkey), len(xdata)]
        seq_d = [xkey, xdata]
        header.tofile(self.mOutput)
        for data in seq_d:
            self.mOutput.write(data)

    def close(self):
        header = array.array('L', [0, 0])
        header.tofile(self.mOutput)
        self.mOutput.close()
        self.mOutput = None

#========================================
class DeepCompReader:
    def __init__(self, fname):
        global DEEP_PREXIX, DEEP_PREXIX_PRE
        self.mFName = fname
        self.mInput = open(fname, "rb")
        line = self.mInput.readline()
        self.mLegacy = (line == DEEP_PREXIX_PRE)
        if not self.mLegacy:
            assert line == DEEP_PREXIX
        line = self.mInput.readline()
        self.mColumns = [bytes(name, encoding = "utf-8")
            for name in json.loads(line.decode(encoding = "utf-8"))]

    def isLegacyMode(self):
        return self.mLegacy

    def getColumnNames(self):
        return self.mColumns

    def close(self):
        self.mInput.close()

    def readOne(self):
        header = array.array('L')
        header.fromfile(self.mInput, len(self.mColumns) + 1)
        if header[0] == 0:
            assert all(cnt == 0 for cnt in header)
            return None, None
        xkey = self.mInput.read(header[0])
        data_seq = [self.mInput.read(cnt)
            for cnt in header[1:]]
        return xkey, data_seq
