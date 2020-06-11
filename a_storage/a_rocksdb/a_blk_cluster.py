import logging
from .a_blocker import ABlocker
from codec.block_support import BytesFieldsSupport
#===============================================
DEBUG_MODE = False

#===============================================
class ABlockerCluster(ABlocker):
    MAX_POS_COUNT = 50

    def __init__(self, schema, properties, key_codec_type):
        ABlocker.__init__(self, schema, properties, key_codec_type,
            use_cache = True, conv_bytes = False, seek_column = True)
        self.mMaxVarCount = self._getProperty("max-var-count")
        self.mLastWriteKey = None
        self.mMaxPosCount = self._getProperty(
            "max-loc-count", self.MAX_POS_COUNT)
        if self.isWriteMode():
            stat_info = self._getProperty("stat", dict())
            self.mCountBlocks = stat_info.get("cluster-blocks", 0)
            self.mCountVariants = stat_info.get("cluster-variants", 0)
        self.mBytesSupp = BytesFieldsSupport(["PosSeq", "bz2"])
        if self.getSchema()._withStr():
            self.mBytesSupp.addConv("bz2")
        self._onDuty()

    def _addWriteStat(self, var_count):
        self.mCountBlocks += 1
        self.mCountVariants += var_count

    def writeCountsAreGood(self, pos_count, var_count):
        if pos_count + 1 >= self.mMaxPosCount:
            return False
        return self.mMaxVarCount is None or var_count + 1 < self.mMaxVarCount

    def getBlockType(self):
        return "cluster"

    def updateWStat(self):
        if not self.isWriteMode():
            return
        stat_info = self._getProperty("stat")
        stat_info["cluster-blocks"] = self.mCountBlocks
        stat_info["cluster-variants"] = self.mCountVariants

    def close(self):
        self.updateWStat()

    def putBlock(self, chrom, pos_seq, main_data_seq):
        self._putData((chrom, pos_seq[-1]),
            self.mBytesSupp.pack([pos_seq] + main_data_seq))

    def openWriteBlock(self, key):
        if (self.mLastWriteKey is not None
                and key[0] == self.mLastWriteKey[0]):
            assert self.mLastWriteKey[1] < key[1]
        self.mLastWriteKey = key
        return _WriteBlock_Cluster(self, key, self.mMaxVarCount is None)

    def openReadBlock(self, key, last_pos = None):
        assert last_pos is None
        with self._seekData(key) as iter_h:
            key_base, data_base = iter_h.getCurrent()
        pos_seq = None
        if key_base is not None and key_base[0] == key[0]:
            seq_data = self.mBytesSupp.unpack(data_base)
            assert seq_data[0][-1] == 0
            pos_seq = [pos + key_base[1] for pos in seq_data[0]]
            if DEBUG_MODE:
                logging.info("Read Block for %s : %s %d/%d (%s)"
                    % (str(key), str(key_base), pos_seq[0], pos_seq[-1],
                    ",".join(str(len(data)) if data is not None else "None"
                        for data in seq_data[1:])))
            return _ReadBlock_Cluster(self, key, pos_seq, seq_data[1:])
        if DEBUG_MODE:
            logging.info("No read block for %s : (next = %s)"
                % (str(key), str(key_base)))
        return _ReadBlock_Cluster(self, key)

#===============================================
class _WriteBlock_Cluster:
    def __init__(self, blocker, key, no_variants):
        self.mBlocker = blocker
        self.mEncodeEnv = self.mBlocker.getSchema().makeDataEncoder()
        self.mChrom = key[0]
        self.mPosSeq = []
        self.mVarCount = 0
        self.mNoVariants = no_variants

    def goodToAdd(self, key):
        chrom, pos = key
        if self.mChrom == chrom and self.mBlocker.writeCountsAreGood(
                len(self.mPosSeq), self.mVarCount):
            return len(self.mPosSeq) == 0 or self.mPosSeq[-1] + 0x100 >= pos
        return False

    def addRecord(self, key, record):
        chrom, pos = key
        assert self.mChrom == chrom and (len(self.mPosSeq) == 0
            or 0 < pos - self.mPosSeq[-1] <= 0x100)
        self.mPosSeq.append(pos)
        self.mEncodeEnv.put(record)
        if self.mNoVariants:
            self.mVarCount += 1
        else:
            self.mVarCount += len(record)

    def finishUp(self):
        if len(self.mPosSeq) > 0:
            self.mBlocker.putBlock(self.mChrom, self.mPosSeq,
                self.mEncodeEnv.result())
            self.mBlocker._addWriteStat(self.mVarCount)
        del self.mEncodeEnv

#===============================================
class _ReadBlock_Cluster:
    def __init__(self, blocker, seek_key, pos_seq = None, data_seq = None):
        self.mBlocker = blocker
        self.mChrom, self.mStartPos = seek_key
        self.mPosSeq = pos_seq
        self.mDataSeq = (self.mBlocker.getSchema().decodeData(data_seq)
            if data_seq is not None else None)

    def goodToRead(self, key, last_pos = None):
        assert last_pos is None
        chrom, pos = key
        if chrom != self.mChrom or pos < self.mStartPos:
            return False
        ret = self.mPosSeq is None or pos <= self.mPosSeq[-1]
        if ret and DEBUG_MODE:
            logging.info("For %s good read block: %s"
                % (str(key), str((self.mChrom, self.mStartPos,
                    self.mPosSeq[-1]) if self.mPosSeq is not None else None)))
        return ret

    def getRecord(self, key, codec, last_pos = None):
        assert last_pos is None
        chrom, pos = key
        assert self.mChrom == chrom
        if self.mDataSeq is not None and pos in self.mPosSeq:
            return self.mDataSeq.get(self.mPosSeq.index(pos))
        return None
