from array import array
from bisect import bisect_left

from .a_blocker import ABlockerIO
from codec import getKeyCodec
#===============================================
class ABlockerIO_PosPager(ABlockerIO):
    def __init__(self, schema, properties, base_key_codec_type):
        ABlockerIO.__init__(self, schema, properties, key_codec_type = "id",
            col_type = "pager", use_cache = True, conv_bytes = False)
        self.mBaseKeyCodec = getKeyCodec(base_key_codec_type)
        self.mLastWriteKey = None
        stat_info = self._getProperty("stat", dict())
        if self.isWriteMode():
            self.mCount = stat_info.get("page-count", 0)
            self.mMaxLen = stat_info.get("page-max-len", 0)
            self.mSumLen = stat_info.get("page-sum-len", 0)
        self._onDuty()

    def getBlockType(self):
        return "page-cluster"

    def updateWStat(self):
        if self.isWriteMode():
            stat_info = self._getProperty("stat")
            stat_info["page-count"] = self.mCount
            stat_info["page-max-len"] = self.mMaxLen
            stat_info["page-sum-len"] = self.mSumLen

    def close(self):
        self.updateWStat()

    def getPageXKey(self, key):
        return self.mBaseKeyCodec.encode(key)[:2]

    def getStartKey(self, page_xkey):
        return self.mBaseKeyCodec.decode(page_xkey + b'\0\0')

    def putPage(self, page_xkey, page_array):
        self._putData(page_xkey, page_array.tobytes())
        self.mCount += 1
        ll = len(page_array)
        self.mSumLen += ll
        if ll > self.mMaxLen:
            self.mMaxLen = ll

    def regPos(self, key):
        self.putRecord(key, None)

    def seekPos(self, key):
        page_xkey = self.getPageXKey(key)
        read_block_h = self._pickCache(page_xkey)
        if read_block_h is None:
            read_block_h = _ReadBlock_PosPager(self,
                page_xkey, self._getData(page_xkey))
            self._pushCache(read_block_h)
        return read_block_h.seekPos(key)

    def openWriteBlock(self, key):
        if (self.mLastWriteKey is not None
                and key[0] == self.mLastWriteKey[0]):
            assert self.mLastWriteKey[1] < key[1]
        self.mLastWriteKey = key
        return _WriteBlock_PosPager(self, self.getPageXKey(key))

#===============================================
class _WriteBlock_PosPager:
    def __init__(self, blocker, page_xkey):
        self.mBlocker = blocker
        self.mArray = array('H')
        self.mPageXKey = page_xkey
        _, self.mStartPos = self.mBlocker.getStartKey(page_xkey)
        self.mCurPos = self.mStartPos - 1

    def goodToAdd(self, key):
        return (self.mBlocker.getPageXKey(key) == self.mPageXKey)

    def addRecord(self, key, record):
        self.regPos(key)

    def regPos(self, key):
        chrom, pos = key
        assert (self.mPageXKey == self.mBlocker.getPageXKey(key)
            and pos > self.mCurPos)
        self.mArray.append(pos - self.mStartPos)
        self.mCurPos = pos

    def finishUp(self):
        if len(self.mArray) > 0:
            self.mBlocker.putPage(self.mPageXKey, self.mArray)

#===============================================
class _ReadBlock_PosPager:
    def __init__(self, blocker, page_xkey, pos_array_bytes = None):
        self.mBlocker = blocker
        self.mPageXKey = page_xkey
        self.mChrom, self.mStartPos = self.mBlocker.getStartKey(page_xkey)
        self.mEndPos = self.mStartPos + 0x10000
        if pos_array_bytes is None:
            self.mArray = []
        else:
            self.mArray = array('H')
            self.mArray.frombytes(pos_array_bytes)
        self.mLen = len(self.mArray)

    def goodToRead(self, page_xkey, last_pos = None):
        return (page_xkey == self.mPageXKey and last_pos is None)

    def seekPos(self, key):
        assert (self.mBlocker.getPageXKey(key) == self.mPageXKey)
        chrom, pos = key
        idx = bisect_left(self.mArray, pos - self.mStartPos)
        if idx < self.mLen:
            ret = self.mArray[idx] + self.mStartPos
            return (chrom, ret), [self.mArray[idx - 1] + self.mStartPos + 1
                if idx > 0 else self.mStartPos, ret + 1]
        return None, [self.mArray[-1] + self.mStartPos
            if self.mLen > 0 else self.mStartPos, self.mEndPos]
