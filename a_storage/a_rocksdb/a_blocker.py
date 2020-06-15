import abc
from .a_io import AIO_ColumnHandler, ABlockCache, AIO_Iterator
from codec import getKeyCodec
from codec.block_support import BytesFieldsSupport
#===============================================
class ABlockerIO:
    def __init__(self, schema, properties, key_codec_type,
            col_type = "base", col_name = None, conv_bytes = False,
            seek_column = False, use_cache = False):
        self.mSchema = schema
        self.mProperties = properties
        self.mKeyCodec = getKeyCodec(key_codec_type)
        self.mDescr = {"block-type": self.mProperties["block-type"]}
        self.mOnDuty = False
        self.mSeekColumn = seek_column
        if self.mSchema.isWriteMode():
            self.mWriteBlockH = None
        col_key = col_type + "-col-options"
        col_attrs = self._getProperty(col_key)
        if col_attrs is None:
            col_attrs = dict()
            self._updateProperty(col_key, col_attrs)
        if col_name is None:
            col_name = "%s_%s" % (self.mSchema.getName(), col_type)
        self.mColumnH = AIO_ColumnHandler(
            self.mSchema.getDbConnector().regColumn(
                col_name, col_attrs, seek_column),
            conv_bytes, col_attrs.get("-compress"))
        self.mReadCache = (ABlockCache(self._getProperty("cache-size"))
            if use_cache else None)

    def getSchema(self):
        return self.mSchema

    def isWriteMode(self):
        return self.mSchema.isWriteMode()

    def _getProperty(self, name, default_value = None):
        if name in self.mDescr:
            return self.mDescr[name]
        if name in self.mProperties:
            self.mDescr[name] = self.mProperties[name]
        else:
            self.mDescr[name] = default_value
        return self.mDescr[name]

    def _updateProperty(self, key, val):
        self.mDescr[key] = val

    def _onDuty(self):
        assert not self.mOnDuty
        unused = set(self.mProperties.keys()) - set(self.mDescr.keys())
        assert not self.isWriteMode() or len(unused) == 0, (
            "Lost option(s) for %s blocker: %s"
            % (self.mSchema.getName(), ", ".join(sorted(unused))))
        self.mOnDuty = True

    def getDescr(self):
        return self.mDescr

    def getDBKeyType(self):
        return self.mKeyCodec.getType()

    def getColumnH(self):
        return self.mColumnH

    @abc.abstractmethod
    def getBlockType(self):
        return None

    def getXKey(self, key):
        return self.mKeyCodec.encode(key)

    def decodeXKey(self, xkey):
        return self.mKeyCodec.decode(xkey)

    def _putData(self, key, data, use_encode = True):
        self.mSchema.getDbConnector().putData(
            self.getXKey(key), self.mColumnH, data, use_encode)

    def _getData(self, key):
        return self.mSchema.getDbConnector().getData(
            self.getXKey(key), self.mColumnH)

    def _seekData(self, key):
        assert self.mSeekColumn
        return AIO_Iterator(self.mSchema.getDbConnector().seekIt(
            self.getXKey(key)), self.mKeyCodec, self.mColumnH)

    def _pickCache(self, key, last_pos = None):
        if self.mReadCache:
            return self.mReadCache.pick(key, last_pos)
        return None

    def _pushCache(self, read_block_h):
        if self.mReadCache is not None:
            self.mReadCache.push(read_block_h)

    def putRecord(self, key, record):
        if (self.mWriteBlockH is not None
                and not self.mWriteBlockH.goodToAdd(key)):
            self.mWriteBlockH.finishUp()
            self.mWriteBlockH = None
        if self.mWriteBlockH is None:
            self.mWriteBlockH = self.openWriteBlock(key)
        self.mWriteBlockH.addRecord(key, record)

    def getRecord(self, key, last_pos = None):
        if key == ('chr20', 141054):
            print("here")
        assert self.mSchema.getDbConnector().properAccess()
        read_block_h = self._pickCache(key, last_pos)
        if read_block_h is None:
            read_block_h = self.openReadBlock(key, last_pos)
            self._pushCache(read_block_h)
        return read_block_h.getRecord(key, last_pos)

    @abc.abstractmethod
    def openWriteBlock(self, key):
        return None

    @abc.abstractmethod
    def openReadBlock(self, key, last_pos = None):
        return None

    def updateWStat(self):
        pass

    def flush(self):
        if self.mSchema.isWriteMode() and self.mWriteBlockH is not None:
            self.mWriteBlockH.finishUp()
            self.mWriteBlockH = None

    def close(self):
        self.flush()
        self.updateWStat()

    def normalizeSample(self, key, record):
        return record

#===============================================
class ABlockerIO_Complex(ABlockerIO):
    def __init__(self, schema, properties, key_codec_type,
            col_type = "base", col_name = None, conv_bytes = False,
            seek_column = False, use_cache = False, pre_part = None):
        ABlockerIO.__init__(self, schema, properties, key_codec_type,
            col_type, col_name, conv_bytes, seek_column, use_cache)
        stat_info = self._getProperty("stat", dict())
        parts_tp_seq = ["bz2"]
        if pre_part:
            parts_tp_seq.insert(0, pre_part)
        self.mBytesSupp = BytesFieldsSupport(parts_tp_seq,
            stat_info.get("complex-max-part-sizes", []),
            stat_info.get("complex-sum-part-sizes", []))
        if self.getSchema()._withStr():
            self.mBytesSupp.addConv("bz2")

    def updateWStat(self):
        if self.isWriteMode():
            stat_info = self._getProperty("stat")
            stat_info["complex-part-sizes"] = self.mBytesSupp.getStatMaxSeq()
            stat_info["complex-part-sizes"] = self.mBytesSupp.getStatSumSeq()

    def _putBlock(self, key, all_data_seq):
        self._putData(key, self.mBytesSupp.pack(all_data_seq))

    def _unpack(self, xdata):
        return self.mBytesSupp.unpack(xdata)

    def _getBlock(self, key):
        xdata = self._getData(key)
        if xdata is None:
            return None
        return self._unpack(xdata)

#===============================================
#===============================================
class ABlockerIO_Single(ABlockerIO):
    def __init__(self, schema, properties, key_codec_type,
            col_type = "base", col_name = None, conv_bytes = False,
            seek_column = False, use_cache = False):
        ABlockerIO.__init__(self, schema, properties, key_codec_type,
            col_type, col_name, conv_bytes, seek_column, use_cache)
        self._onDuty()

    def getBlockType(self):
        return "single"

    def updateWStat(self):
        if self.mSchema.isWriteMode():
            stat_info = self._getProperty("stat")
            stat_info["total"] = self.mSchema.getTotal()

    def openWriteBlock(self, key):
        return _WriteBlock_Single(self, key)

    def openReadBlock(self, key, last_pos = None):
        return _ReadBlock_Single(self, key)


#===============================================
class _WriteBlock_Single:
    def __init__(self, blocker, key):
        self.mBlocker = blocker
        self.mKey = key

    def goodToAdd(self, key):
        return False

    def addRecord(self, key, record):
        assert key == self.mKey
        self.mBlocker._putData(key, record)

    def finishUp(self):
        pass

#===============================================
class _ReadBlock_Single:
    def __init__(self, blocker, key):
        self.mKey = key
        self.mData = blocker._getData(key)

    def goodToRead(self, key, last_pos = None):
        assert last_pos is None
        return self.mKey == key

    def getRecord(self, key, last_pos = None):
        assert last_pos is None
        assert self.mKey == key
        return self.mData
