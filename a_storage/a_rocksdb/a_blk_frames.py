from .a_blocker import ABlocker
from codec.block_support import BytesFieldsSupport
#===============================================
class ABlockerFrameIndex(ABlocker):
    def __init__(self, schema, properties, key_codec_type):
        ABlocker.__init__(self, schema, properties, key_codec_type,
            use_cache = True, conv_bytes = False, seek_column = True)
        self.mPosKeys = self._getProperty("pos-keys")
        self.mCurWriterKey = None
        if self.isWriteMode():
            stat_info = self._getProperty("stat", dict())
            self.mCountBlocks = stat_info.get("frames-blocks", 0)
            self.mMaxBlockLen = stat_info.get("frames-max-block-len", 0)
            self.mCountEmptyBlocks = stat_info.get("frames-blocks-empty", 0)
        self.mBytesSupp = BytesFieldsSupport(["bz2"])
        if self.getSchema()._withStr():
            self.mBytesSupp.addConv("bz2")
        self._onDuty()

    def getBlockType(self):
        return "frame"

    def updateWStat(self):
        if not self.isWriteMode():
            return
        stat_info = self._getProperty("stat")
        stat_info["frames-blocks"] = self.mCountBlocks
        stat_info["frames-max-block-len"] = self.mMaxBlockLen
        stat_info["frames-blocks-empty"] = self.mCountEmptyBlocks

    def getPosKeys(self):
        return self.mPosKeys

    def putBlock(self, key, main_data_seq):
        self._putData(key, self.mBytesSupp.pack(main_data_seq))

    def _addWriteStat(self, block_len):
        if block_len > 0:
            self.mCountBlocks += 1
            self.mMaxBlockLen = max(self.mMaxBlockLen, block_len)
        else:
            self.mCountEmptyBlocks += 1

    def openWriteBlock(self, key):
        return _WriteBlock_Frame(self, key)

    def openReadBlock(self, key, last_pos = None):
        if last_pos is not None:
            assert key[1] <= last_pos
        chrom, init_pos = key

        seek_pos_start, seek_pos_end = None, None
        portions = []
        with self._seekData(key) as iter_h:
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
        for _, seek_data in portions:
            decoded = self.getSchema().decodeData(
                self.mBytesSupp.unpack(seek_data))
            list_data += decoded.get(0)

        return _ReadBlock_Frame(self, chrom, init_pos,
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
class _WriteBlock_Frame:
    def __init__(self, blocker, key):
        self.mBlocker = blocker
        self.mEncodeEnv = self.mBlocker.getSchema().makeDataEncoder()
        self.mKey = key
        self.mBlockLen = None

    def goodToAdd(self, key):
        return key == self.mKey and self.mBlockLen is None

    def addRecord(self, key, record):
        assert key == self.mKey
        assert self.mBlockLen is None
        self.mEncodeEnv.put(record)
        self.mBlockLen = len(record)

    def finishUp(self):
        if self.mBlockLen is not None:
            self.mBlocker.putBlock(self.mKey, self.mEncodeEnv.result())
            self.mBlocker._addWriteStat(self.mBlockLen)
        del self.mEncodeEnv

#===============================================
class _ReadBlock_Frame:
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
