#===============================================
class BlockerSegment():
    def __init__(self, master_io):
        self.mIO = master_io
        self.mPosFrame = self.mIO._getProperty("pos-frame")
        self.mCurWriterKey = None
        if self.mIO.isWriteMode():
            self.mCountSegments = 0
            self.mCountPosGaps = 0

    def _addWriteStat(self, count_pos_gaps):
        self.mCountSegments += 1
        self.mCountPosGaps  += count_pos_gaps

    def getType(self):
        return "segment"

    def close(self):
        if self.mIO.isWriteMode():
            self.mIO._updateProperty("stat", {
                "segments": self.mCountSegments,
                "pos-gaps": self.mCountPosGaps})

    def getIO(self):
        return self.mIO

    def basePos(self, pos):
        return pos - (pos % self.mPosFrame)

    def isGoodKey(self, base_chrom, base_pos, key):
        chrom, pos = key
        return (base_chrom == chrom
            and base_pos <= pos < base_pos + self.mPosFrame)

    def createWriteBlock(self, encode_env, key, codec):
        if (self.mCurWriterKey is not None
                and key[0] == self.mCurWriterKey[0]):
            assert self.mCurWriterKey[1] < key[1]
        self.mCurWriterKey = key
        return _WriteSegmentBlock(self, encode_env, key, codec.isAtomic())

    def createReadBlock(self, decode_env_class, key, codec):
        return _ReadSegmentBlock(self, decode_env_class, key, codec.isAtomic())

#===============================================
class _WriteSegmentBlock:
    def __init__(self, blocker, encode_env, key, no_gap):
        self.mBlocker = blocker
        self.mEncodeEnv = encode_env
        self.mChrom, pos = key
        self.mBasePos = self.mBlocker.basePos(pos)
        self.mCheckStartGap = not no_gap
        self.mCurPos = self.mBasePos
        self.mCountPosGaps = 0

    def goodToWrite(self, key):
        return self.mBlocker.isGoodKey(self.mChrom, self.mBasePos, key)

    def addRecord(self, key, record, codec):
        chrom, pos = key
        assert self.mChrom == chrom and self.mCurPos <= pos
        if self.mCheckStartGap:
            if pos > self.mCurPos:
                self.mEncodeEnv.putValueStr(str(pos - self.mCurPos))
                self.mCurPos = pos
            self.mCheckStartGap = False
        while self.mCurPos < pos:
            self.mEncodeEnv.putValueStr('')
            self.mCurPos += 1
            self.mCountPosGaps += 1
        self.mEncodeEnv.put(record, codec)
        self.mCurPos += 1

    def finishUp(self):
        if self.mCurPos != self.mBasePos:
            self.mBlocker.getIO()._putColumns(
                (self.mChrom, self.mBasePos), self.mEncodeEnv.result())
            self.mBlocker._addWriteStat(self.mCountPosGaps)
        del self.mEncodeEnv

#===============================================
class _ReadSegmentBlock:
    def __init__(self, blocker, decode_env_class, key, no_gap):
        self.mBlocker = blocker
        self.mChrom, pos = key
        self.mBasePos = self.mBlocker.basePos(pos)
        data_seq = self.mBlocker.getIO()._getColumns((self.mChrom, self.mBasePos))
        if data_seq[0] is None:
            self.mDecodeEnv = None
            return
        self.mDecodeEnv = decode_env_class(data_seq)
        self.mGapCount = 0
        if not no_gap:
            check_val = self.mDecodeEnv.getValueStr(0)
            if check_val.isdigit():
                self.mGapCount = int(check_val)
                assert self.mGapCount > 0

    def goodToRead(self, key):
        return self.mBlocker.isGoodKey(self.mChrom, self.mBasePos, key)

    def getRecord(self, key, codec):
        chrom, pos = key
        assert self.mChrom == chrom
        if self.mDecodeEnv is None:
            return None
        idx = key[1] - self.mBasePos
        assert 0 <= idx
        if self.mGapCount > 0:
            idx -= self.mGapCount - 1
        if idx < 0 or idx >= len(self.mDecodeEnv):
            return None
        return self.mDecodeEnv.get(idx, codec)
