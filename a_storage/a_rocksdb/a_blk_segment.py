from .a_blocker import ABlocker
from codec.block_support import BytesFieldsSupport
#===============================================
class ABlockerSegment(ABlocker):
    def __init__(self, schema, properties, key_codec_type):
        ABlocker.__init__(self, schema, properties, key_codec_type,
            use_cache = True, conv_bytes = False, seek_column = False)
        self.mPosFrame = self._getProperty("pos-frame")
        self.mLastWriteKey = None
        if self.isWriteMode():
            stat_info = self._getProperty("stat", dict())
            self.mCountSegments = stat_info.get("segments", 0)
            self.mCountPosGaps = stat_info.get("seg-pos-gaps", 0)
        self.mBytesSupp = BytesFieldsSupport(["bz2"])
        if self.getSchema()._withStr():
            self.mBytesSupp.addConv("bz2")
        self._onDuty()

    def _addWriteStat(self, count_pos_gaps):
        self.mCountSegments += 1
        self.mCountPosGaps  += count_pos_gaps

    def getBlockType(self):
        return "segment"

    def updateWStat(self):
        if not self.isWriteMode():
            return
        stat_info = self._getProperty("stat")
        stat_info["segments"] = self.mCountSegments
        stat_info["seg-pos-gaps"] = self.mCountPosGaps

    def basePos(self, pos):
        return pos - (pos % self.mPosFrame)

    def isGoodKey(self, base_chrom, base_pos, key):
        chrom, pos = key
        return (base_chrom == chrom
            and base_pos <= pos < base_pos + self.mPosFrame)

    def putBlock(self, key, main_data_seq):
        self._putData(key, self.mBytesSupp.pack(main_data_seq))

    def getBlock(self, key):
        xdata = self._getData(key)
        if xdata is None:
            return None
        return self.mBytesSupp.unpack(xdata)

    def openWriteBlock(self, key):
        if (self.mLastWriteKey is not None
                and key[0] == self.mLastWriteKey[0]):
            assert self.mLastWriteKey[1] < key[1]
        self.mLastWriteKey = key
        return _WriteBlock_Segment(self, key)

    def openReadBlock(self, key, last_pos = None):
        assert last_pos is None
        return _ReadBlock_Segment(self, key)

#===============================================
class _WriteBlock_Segment:
    def __init__(self, blocker, key):
        self.mBlocker = blocker
        self.mChrom, pos = key
        self.mBasePos = self.mBlocker.basePos(pos)
        self.mEncodeEnv = self.mBlocker.getSchema().makeDataEncoder()
        self.mCurPos = self.mBasePos
        self.mCountPosGaps = 0

    def goodToAdd(self, key):
        return self.mBlocker.isGoodKey(self.mChrom, self.mBasePos, key)

    def addRecord(self, key, record):
        chrom, pos = key
        assert self.mChrom == chrom and self.mCurPos <= pos
        while self.mCurPos < pos:
            self.mEncodeEnv.put(None)
            self.mCurPos += 1
            self.mCountPosGaps += 1
        self.mEncodeEnv.put(record)
        self.mCurPos += 1

    def finishUp(self):
        if self.mCurPos != self.mBasePos:
            self.mBlocker.putBlock(
                (self.mChrom, self.mBasePos), self.mEncodeEnv.result())
            self.mBlocker._addWriteStat(self.mCountPosGaps)
        del self.mEncodeEnv

#===============================================
class _ReadBlock_Segment:
    def __init__(self, blocker, key):
        self.mBlocker = blocker
        self.mChrom, pos = key
        self.mBasePos = self.mBlocker.basePos(pos)
        data_seq = self.mBlocker.getBlock((self.mChrom, self.mBasePos))
        self.mDataSeq = (self.mBlocker.getSchema().decodeData(data_seq)
            if data_seq is not None else None)

    def goodToRead(self, key, last_pos = None):
        assert last_pos is None
        return self.mBlocker.isGoodKey(self.mChrom, self.mBasePos, key)

    def getRecord(self, key, last_pos = None):
        assert last_pos is None
        chrom, pos = key
        assert self.mChrom == chrom
        if self.mDataSeq is None:
            return None
        idx = key[1] - self.mBasePos
        assert 0 <= idx
        if idx < 0 or idx >= len(self.mDataSeq):
            return None
        return self.mDataSeq.get(idx)
