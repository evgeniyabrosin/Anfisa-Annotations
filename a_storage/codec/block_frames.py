#===============================================
class BlockerFrameIndex():
    def __init__(self, master_io):
        self.mIO = master_io
        self.mPosKeys = self.mIO._getProperty("pos-keys")
        self.mCurWriterKey = None
        if self.mIO.isWriteMode():
            self.mCountBlocks = 0
            self.mMaxBlockLen = 0
            self.mCountEmptyBlocks = 0

    def getType(self):
        return "frame-idx"

    def close(self):
        if self.mIO.isWriteMode():
            self.mIO._updateProperty("stat", {
                "blocks": self.mCountBlocks,
                "max-block-len": self.mMaxBlockLen,
                "blocks-empty": self.mCountEmptyBlocks})

    def getIO(self):
        return self.mIO

    def getPosKeys(self):
        return self.mPosKeys

    def _addWriteStat(self, block_len):
        if block_len > 0:
            self.mCountBlocks += 1
        else:
            self.mCountEmptyBlocks += 1
            self.mMaxBlockLen = max(self.mMaxBlockLen, block_len)

    def createWriteBlock(self, encode_env, key, codec):
        return _WriteFrameBlock(self, encode_env, key)

    def createReadBlock(self, decode_env_class, key, codec):
        columns = self.mIO.getColumns()
        key_base, data_base = self.mIO._seekColumn(key, columns[0])
        if key_base is None or key_base[0] != key[0]:
            return _ReadFrameBlock(self, key)
        data_seq = [data_base]
        if len(columns) > 0:
            data_seq += self.mIO._getColumns(key, columns[1:])
        list_data = decode_env_class(data_seq).get(0, codec)
        return _ReadFrameBlock(self, key_base, key[1], list_data)

#===============================================
class _WriteFrameBlock:
    def __init__(self, blocker, encode_env, key):
        self.mBlocker = blocker
        self.mEncodeEnv = encode_env
        self.mKey = key
        self.mEmpty = True

    def goodToWrite(self, key):
        return key == self.mKey and self.mEmpty

    def addRecord(self, key, record, codec):
        assert key == self.mKey
        assert self.mEmpty
        self.mEncodeEnv.put(record, codec)
        self.mEmpty = False

    def finishUp(self):
        if not self.mEmpty:
            res_list_seq = self.mEncodeEnv.result()
            self.mBlocker.getIO()._putColumns(
                self.mKey, res_list_seq)
            self.mBlocker._addWriteStat(len(res_list_seq[0]))
        del self.mEncodeEnv

#===============================================
class _ReadFrameBlock:
    def __init__(self, blocker, seek_key, init_pos = None, list_data = None):
        self.mBlocker = blocker
        self.mChrom, self.mEndPos = seek_key
        self.mInitPos = init_pos
        self.mListData = list_data

    def goodToRead(self, key):
        chrom, pos = key
        if chrom != self.mChrom or pos > self.mEndPos:
            return False
        return self.mInitPos is None or self.mInitPos <= pos

    def getRecord(self, key, codec):
        chrom, pos = key
        assert self.mChrom == chrom
        if self.mListData is None:
            return []
        ret = []
        start_key, end_key = self.mBlocker.getPosKeys()
        for data in self.mListData:
            if data[start_key] <= pos <= data[end_key]:
                ret.append(data)
        return ret
