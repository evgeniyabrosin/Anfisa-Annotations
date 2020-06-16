import logging
from .a_blocker import ABlockerIO_Complex

DEBUG = False
#===============================================
class ABlockerIO_Segment(ABlockerIO_Complex):
    def __init__(self, schema, properties, key_codec_type):
        ABlockerIO_Complex.__init__(self, schema, properties, key_codec_type,
            use_cache = True, conv_bytes = False, seek_column = False)
        self.mPosFrame = self._getProperty("pos-frame")
        self.mLastWriteKey = None
        if self.isWriteMode():
            stat_info = self._getProperty("stat", dict())
            self.mCountSegments = stat_info.get("segments", 0)
            self.mCountPosGaps = stat_info.get("seg-pos-gaps", 0)
        self._onDuty()

    def getBlockType(self):
        return "segment"

    def updateWStat(self):
        ABlockerIO_Complex.updateWStat(self)
        if self.isWriteMode():
            stat_info = self._getProperty("stat", dict())
            stat_info["segments"] = self.mCountSegments
            stat_info["seg-pos-gaps"] = self.mCountPosGaps

    def basePos(self, pos):
        return pos - (pos % self.mPosFrame)

    def isGoodKey(self, base_chrom, base_pos, key):
        chrom, pos = key
        return (base_chrom == chrom
            and base_pos <= pos < base_pos + self.mPosFrame)

    def putBlock(self, key, main_data_seq, count_pos_gaps):
        self._putBlock(key, main_data_seq)
        self.mCountSegments += 1
        self.mCountPosGaps  += count_pos_gaps

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
            self.mBlocker.putBlock((self.mChrom, self.mBasePos),
                self.mEncodeEnv.result(), self.mCountPosGaps)
        del self.mEncodeEnv

#===============================================
class _ReadBlock_Segment:
    def __init__(self, blocker, key):
        self.mBlocker = blocker
        self.mChrom, pos = key
        self.mBasePos = self.mBlocker.basePos(pos)
        data_seq = self.mBlocker._getBlock((self.mChrom, self.mBasePos))
        self.mDataSeq = (self.mBlocker.getSchema().decodeData(data_seq)
            if data_seq is not None else None)

    def goodToRead(self, key, last_pos = None):
        assert last_pos is None
        return self.mBlocker.isGoodKey(self.mChrom, self.mBasePos, key)

    def getRecord(self, key, last_pos = None):
        assert last_pos is None
        chrom, pos = key
        assert self.mChrom == chrom
        if DEBUG:
            logging.info("For key=%s block from %d size=%s"
                % (str(key), self.mBasePos, str(len(self.mDataSeq))
                    if self.mDataSeq is not None else "None"))
        if self.mDataSeq is None:
            return None
        idx = pos - self.mBasePos
        assert 0 <= idx < len(self.mDataSeq)
        return self.mDataSeq.get(idx)
