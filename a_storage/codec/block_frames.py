from ._block_agent import _BlockAgent
#===============================================
class BlockerFrameIndex(_BlockAgent):
    def __init__(self, master_io):
        _BlockAgent.__init__(self, master_io)
        self.mPosKeys = self._getProperty("pos-keys")
        self.mCurWriterKey = None
        if self.isWriteMode():
            stat_info = self._getProperty("stat")
            self.mCountBlocks = stat_info.get("frames-blocks", 0)
            self.mMaxBlockLen = stat_info.get("frames-max-block-len", 0)
            self.mCountEmptyBlocks = stat_info.get("frames-blocks-empty", 0)

    def getType(self):
        return "frame-idx"

    def updateWStat(self):
        if not self.isWriteMode():
            return
        stat_info = self._getProperty("stat")
        stat_info["frames-blocks"] = self.mCountBlocks
        stat_info["frames-max-block-len"] = self.mMaxBlockLen
        stat_info["frames-blocks-empty"] = self.mCountEmptyBlocks

    def getPosKeys(self):
        return self.mPosKeys

    def _addWriteStat(self, block_len):
        if block_len > 0:
            self.mCountBlocks += 1
            self.mMaxBlockLen = max(self.mMaxBlockLen, block_len)
        else:
            self.mCountEmptyBlocks += 1

    def createWriteBlock(self, encode_env, key, codec):
        return _WriteFrameBlock(self, encode_env, key)

    def createReadBlock(self, decode_env_class, key, codec, last_pos = None):
        if last_pos is not None:
            assert key[1] <= last_pos
        main_columns = self.getIO().getMainColumnSeq()
        chrom, init_pos = key

        seek_pos_start, seek_pos_end = None, None
        portions = []
        with self.getIO().seekIt(key) as iter_h:
            seek_key, seek_data = iter_h.getCurrent()
            if seek_key is not None and seek_key[0] == chrom:
                seek_pos_start = seek_pos_end = seek_key[1]
                portions.append([seek_key, seek_data])
            while (last_pos is not None and seek_pos_end is not None
                    and seek_pos_end < last_pos):
                iter_h.seekNext()
                seek_key, seek_data = iter_h.getCurrent()
                if seek_key is not None and seek_key[0] == chrom:
                    seek_pos_end = seek_key[1]
                    portions.append([seek_key, seek_data])
                else:
                    seek_pos_end = None
        list_data = []
        for seek_key, seek_data in portions:
            data_seq = [seek_data]
            if len(main_columns) > 0:
                data_seq += self.getIO()._getColumns(
                    seek_key, main_columns[1:])
            list_data += decode_env_class(data_seq).get(0, codec)

        return _ReadFrameBlock(self, chrom, init_pos,
            seek_pos_start, seek_pos_end, list_data)

    def normalizeSample(self, key, record):
        _, pos = key
        start_key, end_key = self.mPosKeys
        ret = []
        for data in record:
            if data[start_key] <= pos <= data[end_key]:
                ret.append(data)
        return ret

#===============================================
class _WriteFrameBlock:
    def __init__(self, blocker, encode_env, key):
        self.mBlocker = blocker
        self.mEncodeEnv = encode_env
        self.mKey = key
        self.mBlockLen = None

    def goodToWrite(self, key):
        return key == self.mKey and self.mBlockLen is None

    def addRecord(self, key, record, codec):
        assert key == self.mKey
        assert self.mBlockLen is None
        self.mEncodeEnv.put(record, codec)
        self.mBlockLen = len(record)

    def finishUp(self):
        if self.mBlockLen is not None:
            res_list_seq = self.mEncodeEnv.result()
            self.mBlocker.getIO()._putColumns(
                self.mKey, res_list_seq)
            self.mBlocker._addWriteStat(self.mBlockLen)
        del self.mEncodeEnv

#===============================================
class _ReadFrameBlock:
    def __init__(self, blocker, chrom, init_pos,
            seek_pos = None, end_pos = None, list_data = None):
        self.mBlocker = blocker
        self.mChrom = chrom
        self.mInitPos = init_pos
        self.mSeekPos = seek_pos
        self.mEndPos = end_pos
        self.mListData = list_data

    def goodToRead(self, key, last_pos = None):
        chrom, pos = key
        if last_pos is not None:
            assert pos <= last_pos
        if chrom != self.mChrom or pos < self.mInitPos:
            return False
        if self.mEndPos is not None:
            if pos > self.mEndPos:
                return False
            if last_pos is not None and last_pos > self.mEndPos:
                return False
        return True

    def getRecord(self, key, codec, last_pos = None):
        if not self.mListData:
            return []
        chrom, pos = key
        assert self.mChrom == chrom
        ret = []
        start_key, end_key = self.mBlocker.getPosKeys()
        if last_pos is not None:
            assert last_pos >= pos
            for data in self.mListData:
                if max(data[start_key], pos) <= min(data[end_key], last_pos):
                    ret.append(data)
        else:
            for data in self.mListData:
                if data[start_key] <= pos <= data[end_key]:
                    ret.append(data)
        return ret
