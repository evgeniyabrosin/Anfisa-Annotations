import json
from threading import Lock

from codec import createBlockCodec, getKeyCodec
#========================================
class AIOController:
    READ_BLOCK_CACHE_SIZE = 3

    def __init__(self, schema, dbname, properties):
        self.mSchema = schema
        self.mDbName = dbname
        self.mProperties = properties
        self.mDescr = dict()
        self.mOnDuty = False
        self.mKeyCodec = getKeyCodec(self.mSchema.getProperty("key"))

        self.mDbConnector = self.mSchema.getStorage().openConnection(
            dbname, self.mSchema.isWriteMode())
        self.mColNames = []
        self.mWithBase = self.mSchema.isOptionRequired("base")
        if self.mWithBase:
            self.mColNames.append(self._regColumn("base"))
        self.mWithStr = self.mSchema.isOptionRequired("str")
        if self.mWithStr:
            self.mColNames.append(self._regColumn("str"))

        if self.mSchema.isWriteMode():
            self.mWriteBlockH = None
            stat_info = self._getProperty("stat", dict())
            if self.mWithBase:
                self.mBaseBlockCount = stat_info.get("base-blocks", 0)
                self.mBaseTotalLen = stat_info.get("base-total-l", 0)
                self.mBaseMaxLen = stat_info.get("base-max-l", 0)
            if self.mWithStr:
                self.mStrTotalLen = stat_info.get("str-total-l", 0)
                self.mStrMaxLen = stat_info.get("str-max-len", 0)
                self.mStrTotalCount = stat_info.get("str-total-count", 0)
                self.mStrMaxCount = stat_info.get("str-max-count", 0)

        block_type = self._getProperty("block-type")
        if block_type is not None:
            self.mBlockCodec = createBlockCodec(
                self, self._getProperty("block-type"))
            self.mReadLock = Lock()
            self.mReadCache = []
            self.mReadCacheSize = self._getProperty("cache-size",
                self.READ_BLOCK_CACHE_SIZE)
        else:
            self.mBlockCodec = None
        self._onDuty()

    def _getProperty(self, name, default_value = None):
        if name in self.mDescr:
            return self.mDescr[name]
        if name in self.mProperties:
            self.mDescr[name] = self.mProperties[name]
        else:
            self.mDescr[name] = default_value
        return self.mDescr[name]

    def _regColumn(self, col_type, col_name = None):
        col_key = col_type + "-col-options"
        col_attrs = self._getProperty(col_key)
        if col_attrs is None:
            col_attrs = self.mSchema.getStorage().getDefaulColumnAttrs(
                col_type).copy()
            self._updateProperty(col_key, col_attrs)
        if col_name is None:
            col_name = "%s_%s" % (self.mSchema.getName(), col_type)
        return self.mDbConnector._regColumn(col_name, col_attrs)

    def _updateProperty(self, key, val):
        self.mDescr[key] = val

    def _onDuty(self):
        assert not self.mOnDuty
        unused = set(self.mProperties.keys()) - set(self.mDescr.keys())
        assert not self.isWriteMode() or len(unused) == 0, (
            "Lost option(s) for %s blocker: %s"
            % (self.mSchema.getName(), ", ".join(sorted(unused))))
        self.mOnDuty = True

    def _addWriteStat(self, base_len, str_len = None, str_count = None):
        assert self.mWithBase
        if self.mWithBase:
            self.mBaseBlockCount += 1
            self.mBaseTotalLen += base_len
            self.mBaseMaxLen = max(self.mBaseMaxLen, base_len)
        if self.mWithStr:
            self.mStrTotalLen += str_len
            self.mStrMaxLen = max(self.mStrMaxLen, str_len)
            self.mStrTotalCount += str_count
            self.mStrMaxCount = max(self.mStrMaxCount, str_count)

    def isWriteMode(self):
        return self.mSchema.isWriteMode()

    def getDbName(self):
        return self.mDbName

    def flush(self):
        if (self.mWriteBlockH is not None):
            self.mWriteBlockH.finishUp()
            self.mWriteBlockH = None

    def updateWStat(self):
        if not self.mSchema.isWriteMode():
            return
        stat_info = self._getProperty("stat")
        stat_info["total"] = self.mSchema.getTotal()
        if self.mWithBase:
            stat_info["base-blocks"] = self.mBaseBlockCount
            stat_info["base-total-l"] = self.mBaseTotalLen
            stat_info["base-max-l"] = self.mBaseMaxLen
        if self.mWithStr:
            stat_info["str-total-l"] = self.mStrTotalLen
            stat_info["str-max-len"] = self.mStrMaxLen
            stat_info["str-total-count"] = self.mStrTotalCount
            stat_info["str-max-count"] = self.mStrMaxCount
        if self.mBlockCodec is not None:
            self.mBlockCodec.updateWStat()

    def close(self):
        self.flush()
        if self.mBlockCodec is not None:
            self.mBlockCodec.close()
        self.updateWStat()
        self.mSchema.getStorage().closeConnection(self.mDbConnector)

    def getDescr(self):
        return self.mDescr

    def getDBKeyType(self):
        return self.mKeyCodec.getType()

    def getColumnNames(self):
        return self.mColNames

    def getXKey(self, key):
        return self.mKeyCodec.encode(key)

    def _putColumns(self, key, data_seq, col_names = None, conv_bytes = True):
        xkey = self.getXKey(key)
        if col_names is None:
            col_names = self.mColNames
        self.mDbConnector.putData(xkey, col_names, data_seq, conv_bytes)

    def _getColumns(self, key, col_names = None, conv_bytes = True):
        xkey = self.getXKey(key)
        if col_names is None:
            col_names = self.mColNames
        return self.mDbConnector.getData(xkey, col_names, conv_bytes)

    def _seekColumn(self, key, col_name, conv_bytes = True):
        return AIO_Iterator(self.mDbConnector.seekData(
            self.getXKey(key), col_name, conv_bytes), self.mKeyCodec)

    def _putRecord(self, key, record, codec):
        encode_env = AEncodeEnv(self, self.mWithStr)
        encode_env.put(record, codec)
        self.mDbConnector.putData(self.getXKey(key),
            self.mColNames, encode_env.result())

    def _getRecord(self, key, codec):
        data_seq = self.mDbConnector.getData(self.getXKey(key), self.mColNames)
        if data_seq[0] is None:
            return None
        decode_env = ADecodeEnv(data_seq)
        return decode_env.get(0, codec)

    def putRecord(self, key, record, codec):
        assert self.mSchema.isWriteMode()
        if (self.mWriteBlockH is not None
                and not self.mWriteBlockH.goodToWrite(key)):
            self.mWriteBlockH.finishUp()
            self.mWriteBlockH = None
        if self.mWriteBlockH is None:
            self.mWriteBlockH = self.mBlockCodec.createWriteBlock(
                AEncodeEnv(self, self.mWithStr), key, codec)
        self.mWriteBlockH.addRecord(key, record, codec)

    def getRecord(self, key, codec, last_pos = None):
        read_block_h = None
        with self.mReadLock:
            for idx, rblock_h in enumerate(self.mReadCache):
                if rblock_h.goodToRead(key, last_pos):
                    read_block_h = rblock_h
                    if idx > 0:
                        del self.mReadCache[idx]
                        self.mReadCache.insert(0, read_block_h)
                    break
        if read_block_h is None:
            read_block_h = self.mBlockCodec.createReadBlock(
                ADecodeEnv, key, codec, last_pos)
            with self.mReadLock:
                self.mReadCache.insert(0, read_block_h)
                while len(self.mReadCache) > self.mReadCacheSize:
                    del self.mReadCache[-1]
        return read_block_h.getRecord(key, codec, last_pos)

    def transformRecord(self, key, record, codec):
        encode_env = AEncodeEnv(None, self.mWithStr)
        encode_env.put(record, codec)
        decode_env = ADecodeEnv(encode_env.result())
        return decode_env.get(0, codec)

#========================================
class AIO_Iterator:
    def __init__(self, db_iter, key_codec):
        self.mIter = db_iter
        self.mKeyCodec = key_codec

    def __enter__(self):
        return self

    def __exit__(self, tp, value, traceback):
        self.mIter.close()

    def getCurrent(self):
        xkey, data = self.mIter.getCurrent()
        if xkey is None:
            return None, None
        return self.mKeyCodec.decode(xkey), data

    def seekNext(self):
        return self.mIter.seekNext()

#========================================
class AEncodeEnv:
    def __init__(self, master, with_str):
        self.mMaster = master
        self.mObjSeq = []
        self.mStrSeq = [] if with_str else None
        self.mIntDict = None

    def addStr(self, txt, repeatable = False):
        if repeatable:
            if self.mIntDict is None:
                self.mIntDict = dict()
            else:
                if txt in self.mIntDict:
                    return self.mIntDict[txt]
        ret = len(self.mStrSeq)
        self.mStrSeq.append(txt)
        if repeatable:
            self.mIntDict[txt] = ret
        return ret

    def put(self, record, codec):
        self.mObjSeq.append(codec.encode(record, self))

    def putValueStr(self, value_str):
        self.mObjSeq.append(value_str)

    def result(self):
        ret = ['\0'.join(self.mObjSeq)]
        stat_info = [len(ret[0])]
        if self.mStrSeq is not None:
            ret.append('\0'.join(self.mStrSeq))
            stat_info += [len(ret[1]), len(self.mStrSeq)]
        if self.mMaster is not None:
            self.mMaster._addWriteStat(*stat_info)
            self.mMaster = None
        return ret

#========================================
class ADecodeEnv:
    def __init__(self, data_seq):
        self.mObjSeq = data_seq[0].split('\0')
        if len(data_seq) > 1 and data_seq[1] is not None:
            self.mStrSeq = data_seq[1].split('\0')
        else:
            self.mStrSeq = None

    def getStr(self, idx):
        return self.mStrSeq[idx]

    def __len__(self):
        return len(self.mObjSeq)

    def getValueStr(self, idx):
        return self.mObjSeq[idx]

    def get(self, idx, codec):
        xdata = self.mObjSeq[idx]
        if not xdata:
            return None
        return codec.decode(json.loads(xdata), self)
