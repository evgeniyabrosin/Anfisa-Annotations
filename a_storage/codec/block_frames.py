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
                "frames-blocks": self.mCountBlocks,
                "frames-max-block-len": self.mMaxBlockLen,
                "frames-blocks-empty": self.mCountEmptyBlocks})

    def getIO(self):
        return self.mIO

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
            pos, last_pos = sorted([key[1], last_pos])
            key = (key[0], pos)
        col_names = self.mIO.getColumnNames()
        chrom, init_pos = key

        seek_pos_start, list_data, seek_pos_end = None, None, None
        with self.mIO._seekColumn(key, col_names[0]) as iter_h:
            seek_key, seek_data = iter_h.getCurrent()
            if seek_key is not None and seek_key[0] == chrom:
                seek_pos_start = seek_pos_end = seek_key[1]
                data_seq = [seek_data]
                if len(col_names) > 0:
                    data_seq += self.mIO._getColumns(seek_key, col_names[1:])
                list_data = decode_env_class(data_seq).get(0, codec)
            while (last_pos is not None and seek_pos_end is not None
                    and seek_pos_end < last_pos):
                iter_h.seekNext()
                seek_key, seek_data = iter_h.getCurrent()
                if seek_key is not None and seek_key[0] == chrom:
                    seek_pos_end = seek_key[1]
                    data_seq = [seek_data]
                    if len(col_names) > 0:
                        data_seq += self.mIO._getColumns(
                            seek_key, col_names[1:])
                    list_data += decode_env_class(data_seq).get(0, codec)
                else:
                    seek_pos_end = None
        return _ReadFrameBlock(self, chrom, init_pos,
            seek_pos_start, seek_pos_end, list_data)

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
        if chrom != self.mChrom:
            return False
        if last_pos is not None:
            pos, last_pos = sorted([pos, last_pos])
        if pos < self.mInitPos:
            return False
        if self.mEndPos is not None:
            if pos > self.mEndPos:
                return False
            if last_pos is not None and last_pos > self.mEndPos:
                return False
        return True

    def getRecord(self, key, codec, last_pos = None):
        if self.mListData is None:
            return []
        chrom, pos = key
        assert self.mChrom == chrom
        ret = []
        start_key, end_key = self.mBlocker.getPosKeys()
        if last_pos is not None:
            pos, last_pos = sorted([pos, last_pos])
            for data in self.mListData:
                if max(data[start_key], pos) <= min(data[end_key], last_pos):
                    ret.append(data)
        else:
            for data in self.mListData:
                if data[start_key] <= pos <= data[end_key]:
                    ret.append(data)
        return ret
