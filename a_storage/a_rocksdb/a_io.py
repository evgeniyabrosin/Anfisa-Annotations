import bz2
from threading import Lock

#========================================
class AIO_ColumnHandler:
    def __init__(self, name, conv_bytes, compression = None):
        self.mName = name
        self.mConvBytes = conv_bytes
        self.mUseBZip = (compression == "bz2")

    def getName(self):
        return self.mName

    def encode(self, data):
        if self.mConvBytes and data is not None:
            data = bytes(data, encoding = "utf-8")
        if self.mUseBZip and data is not None:
            return bz2.compress(data)
        return data

    def decode(self, data):
        if self.mUseBZip and data is not None:
            data = bz2.decompress(data)
        if self.mConvBytes and data is not None:
            return data.decode(encoding = "utf-8")
        return data

#========================================
class AIO_Iterator:
    def __init__(self, db_iter, key_codec, column_h):
        self.mIter = db_iter
        self.mKeyCodec = key_codec
        self.mColumnH = column_h

    def __enter__(self):
        return self

    def __exit__(self, tp, value, traceback):
        self.mIter.detach()

    def getCurrent(self):
        xkey, data = self.mIter.getCurrent()
        if xkey is None:
            return None, None
        if data is not None:
            data = self.mColumnH.decode(data)
        return (self.mKeyCodec.decode(xkey), data)

    def seekNext(self):
        return self.mIter.seekNext()

#========================================
class ABlockCache:
    READ_BLOCK_CACHE_SIZE = 20

    def __init__(self, cache_size = None):
        self.mLock = Lock()
        self.mCache = []
        self.mCacheSize = (cache_size if cache_size is not None
            else self.READ_BLOCK_CACHE_SIZE)

    def pick(self, key, last_pos):
        with self.mLock:
            for idx, rblock_h in enumerate(self.mCache):
                if rblock_h.goodToRead(key, last_pos):
                    if idx > 0:
                        del self.mCache[idx]
                        self.mCache.insert(0, rblock_h)
                    return rblock_h
        return None

    def push(self, rblock_h):
        with self.mLock:
            self.mCache.insert(0, rblock_h)
            while len(self.mCache) > self.mCacheSize:
                self.mCache.pop()
