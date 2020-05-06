#===============================================
class BlockerCluster():
    MAX_POS_COUNT = 50

    def __init__(self, master):
        self.mIO = master
        self.mMaxVarCount = self.mIO._getProperty("max-var-count")
        self.mCurWriterKey = None
        self.mIdxColumns = [self.mIO._regColumn("sgidx")]
        self.mMaxPosCount = self.mIO._getProperty(
            "max-loc-count", self.MAX_POS_COUNT)
        if self.mIO.isWriteMode():
            self.mStatBlocks = 0
            self.mStatVariants = 0

    def _addWriteStat(self, var_count):
        self.mStatBlocks += 1
        self.mStatVariants += var_count

    def writeCountsAreGood(self, pos_count, var_count):
        if pos_count + 1 >= self.mMaxPosCount:
            return False
        return self.mMaxVarCount is None or var_count + 1 < self.mMaxVarCount

    def getType(self):
        return "cluster"

    def getIO(self):
        return self.mIO

    def close(self):
        if self.mIO.isWriteMode():
            self.mIO._updateProperty("stat-blocks", self.mStatBlocks)
            if self.mMaxVarCount is not None:
                self.mIO._updateProperty("stat-variants", self.mStatVariants)

    def addToIndex(self, chrom, pos_seq):
        seq_xdelta = []
        for idx in range(len(pos_seq) - 1, 0, -1):
            delta = pos_seq[idx] - pos_seq[idx - 1] - 1
            seq_xdelta.append(delta.to_bytes(1, byteorder = 'big'))
        xdata = b''.join(seq_xdelta)
        self.mIO._putColumns((chrom, pos_seq[-1]),
            [xdata], self.mIdxColumns, conv_bytes = False)

    def createWriteBlock(self, encode_env, key, codec):
        if (self.mCurWriterKey is not None
                and key[0] == self.mCurWriterKey[0]):
            assert self.mCurWriterKey[1] < key[1]
        self.mCurWriterKey = key
        return _WriteClusterBlock(self, encode_env, key,
            self.mMaxVarCount is None)

    def createReadBlock(self, decode_env_class, key, codec):
        key_base, data_base = self.mIO._seekColumn(key,
            self.mIdxColumns[0], conv_bytes = False)
        pos_seq = None
        if key_base is not None:
            delta_seq = [int.from_bytes(data_base[idx:idx + 1], 'big')
                for idx in range(0, len(data_base))]
            pos_seq = [key_base[1]]
            for delta in delta_seq:
                pos_seq.append(pos_seq[-1] - delta - 1)
            pos_seq = pos_seq[-1::-1]
            data_seq = self.mIO._getColumns(key_base)
            return _ReadClusterBlock(self, key, pos_seq,
                decode_env_class(data_seq))
        return _ReadClusterBlock(self, key)

#===============================================
class _WriteClusterBlock:
    def __init__(self, blocker, encode_env, key, no_variants):
        self.mBlocker = blocker
        self.mEncodeEnv = encode_env
        self.mChrom = key[0]
        self.mPosSeq = []
        self.mVarCount = 0
        self.mNoVariants = no_variants

    def goodToWrite(self, key):
        chrom, pos = key
        if self.mChrom == chrom and self.mBlocker.writeCountsAreGood(
                len(self.mPosSeq), self.mVarCount):
            return len(self.mPosSeq) == 0 or self.mPosSeq[-1] + 0x100 >= pos
        return False

    def addRecord(self, key, record, codec):
        chrom, pos = key
        assert self.mChrom == chrom and (len(self.mPosSeq) == 0
            or 0 < pos - self.mPosSeq[-1] <= 0x100)
        self.mPosSeq.append(pos)
        self.mEncodeEnv.put(record, codec)
        if self.mNoVariants:
            self.mVarCount += 1
        else:
            self.mVarCount += len(record)

    def finishUp(self):
        if len(self.mPosSeq) > 0:
            self.mBlocker.getIO()._putColumns(
                (self.mChrom, self.mPosSeq[-1]), self.mEncodeEnv.result())
            self.mBlocker.addToIndex(self.mChrom, self.mPosSeq)
            self.mBlocker._addWriteStat(self.mVarCount)
        del self.mEncodeEnv

#===============================================
class _ReadClusterBlock:
    def __init__(self, blocker, seek_key, pos_seq = None, decode_env = None):
        self.mBlocker = blocker
        self.mChrom, self.mStartPos = seek_key
        self.mPosSeq = pos_seq
        self.mDecodeEnv = decode_env

    def goodToRead(self, key):
        chrom, pos = key
        if chrom != self.mChrom or pos < self.mStartPos:
            return False
        return self.mPosSeq is None or pos <= self.mPosSeq[-1]

    def getRecord(self, key, codec):
        chrom, pos = key
        assert self.mChrom == chrom
        if self.mDecodeEnv is not None and pos in self.mPosSeq:
            idx = self.mPosSeq.index(pos)
            return self.mDecodeEnv.get(idx, codec)
        return None
