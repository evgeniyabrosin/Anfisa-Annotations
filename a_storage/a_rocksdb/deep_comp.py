import array, json
#========================================
DEEP_PREXIX = b"AStorage/DeepComp-0.1\n"

class DeepCompWriter:
    def __init__(self, fname):
        global DEEP_PREXIX
        self.mFName = fname
        self.mOutput = open(fname, "wb")
        self.mOutput.write(DEEP_PREXIX)
        self.mColumns = None

    def put(self, xkey, col_seq, data_seq):
        c_names = [column_h.getName() for column_h in col_seq]
        if self.mColumns is None:
            self.mColumns = c_names
            rep_columns = json.dumps(
                [nm.decode(encoding = "utf-8") for nm in self.mColumns],
                ensure_ascii = False)
            self.mOutput.write(bytes(rep_columns + '\n', encoding = "utf-8"))
        else:
            assert self.mColumns == c_names, (
                "Only one sequence of columns supported")
        assert len(col_seq) == len(data_seq)
        header = array.array('L')
        header.append(len(xkey))
        seq_d = [xkey]
        for data in data_seq:
            header.append(len(data))
            seq_d.append(data)
        header.tofile(self.mOutput)
        for data in seq_d:
            self.mOutput.write(data)

    def close(self):
        header = array.array('L', [0])
        for _ in self.mColumns:
            header.append(0)
        header.tofile(self.mOutput)
        self.mOutput.close()
        self.mOutput = None

#========================================
class DeepCompReader:
    def __init__(self, fname):
        global DEEP_PREXIX
        self.mFName = fname
        self.mInput = open(fname, "rb")
        line = self.mInput.readline()
        assert line == DEEP_PREXIX
        line = self.mInput.readline()
        self.mColumns = [bytes(name, encoding = "utf-8")
            for name in json.loads(line.decode(encoding = "utf-8"))]

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
