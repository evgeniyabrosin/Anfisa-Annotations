import os, shutil, logging, json
import pyrocksdb as rocksdb

#========================================
class AConnector:
    def __init__(self, storage, name, write_mode = False):
        self.mStorage = storage
        self.mName = name
        self.mRefCount = 0
        self.mFilePath = self.mStorage.getDBFilePath(name)
        self.mWriteMode = write_mode
        self.mColIndex = dict()
        self.mColDescriptors = rocksdb.VectorColumnFamilyDescriptor()
        self.mColDescriptors.append(rocksdb.ColumnFamilyDescriptor
            (rocksdb.DefaultColumnFamilyName, rocksdb.ColumnFamilyOptions()))
        self.mRdOpts = rocksdb.ReadOptions()
        self.mWrOpts = rocksdb.WriteOptions() if self.mWriteMode else None
        self.mDB = rocksdb.DB()
        self.mColHandlers = None
        if self.mWriteMode:
            if os.path.exists(self.mFilePath):
                shutil.rmtree(self.mFilePath)
            os.mkdir(self.mFilePath)
            self.mDB.open(self._dbOptions(), self.mFilePath)

    def _incRefCount(self):
        self.mRefCount += 1

    def _decRefCount(self):
        self.mRefCount -= 1
        return self.mRefCount

    def getName(self):
        return self.mName

    def _dbOptions(self):
        options = rocksdb.Options()
        for key, val in self.mStorage.getDbOptions():
            setattr(options, key, val)
        return options

    def _colOptions(self, col_attrs):
        col_options = rocksdb.ColumnFamilyOptions()
        col_options.OptimizeLevelStyleCompaction()
        for key, val in col_attrs.items():
            if key == "compression":
                col_options.set_compression(val)
            else:
                setattr(col_options, key, val)
        return col_options

    def _regColumn(self, c_name, col_attrs):
        assert self.mColHandlers is None
        col_name = bytes(c_name, encoding = "utf-8")
        assert col_name not in self.mColIndex
        self.mColIndex[col_name] = len(self.mColDescriptors)
        self.mColDescriptors.append(rocksdb.ColumnFamilyDescriptor(
            col_name, self._colOptions(col_attrs)))
        if self.mWriteMode:
            s, cf = self.mDB.create_column_family(
                self._colOptions(col_attrs), col_name)
            del cf
        return col_name

    def activate(self):
        assert self.mColHandlers is None
        if self.mWriteMode:
            self.mDB.close()
            _, self.mColHandlers = self.mDB.open(
                self._dbOptions(), self.mFilePath, self.mColDescriptors)
        else:
            _, self.mColHandlers = self.mDB.open_for_readonly(
                self._dbOptions(), self.mFilePath, self.mColDescriptors)
        self._reportKeys()

    def close(self):
        self._reportKeys()
        if self.mWriteMode:
            compact_opt = rocksdb.CompactRangeOptions()
            for col_h in self.mColHandlers:
                self.mDB.compact_range(compact_opt, col_h, None, None)
        for col_h in self.mColHandlers:
            del col_h
        self.mDB.close()

    def getWriteMode(self):
        return self.mWriteMode

    def getColumnCount(self):
        return len(self.mColNames)

    def _reportKeys(self):
        for col_h in self.mColHandlers:
            seq = []
            x_iter = self.mDB.iterator(self.mRdOpts, col_h)
            x_iter.seek_to_first()
            while x_iter.valid():
                seq.append(x_iter.key().hex())
                if len(seq) >= 10:
                    break
                x_iter.next()
            logging.info("Keys for %s/%s: %s"
                % (self.mName, col_h.get_name(), json.dumps(seq)))

    def putData(self, xkey, col_seq, data_seq, conv_bytes = True):
        assert self.mWriteMode
        for col_name, data in zip(col_seq, data_seq):
            if conv_bytes:
                data = bytes(data, encoding = "utf-8")
            if len(data) > 0:
                col_h = self.mColHandlers[self.mColIndex[col_name]]
                self.mDB.put(self.mWrOpts, col_h, xkey, data)

    def getData(self, xkey, col_seq, conv_bytes = True):
        ret = []
        for col_name in col_seq:
            col_h = self.mColHandlers[self.mColIndex[col_name]]
            blob = self.mDB.get(self.mRdOpts, col_h, xkey)
            data = blob.data if blob.status.ok() else None
            if conv_bytes and data is not None:
                data = data.decode(encoding = "utf-8")
            ret.append(data)
        return ret

    def seekData(self, xkey, col_name, conv_bytes):
        col_h = self.mColHandlers[self.mColIndex[col_name]]
        x_iter = self.mDB.iterator(self.mRdOpts, col_h)
        x_iter.seek(xkey)
        return _AIterator(x_iter, conv_bytes)

#========================================
class _AIterator:
    def __init__(self, x_iter, conv_bytes):
        self.mIter = x_iter
        self.mConvBytes = conv_bytes

    def getCurrent(self):
        if self.mIter.valid():
            ret_key, ret_data = self.mIter.key(), self.mIter.value()
            if self.mConvBytes and ret_data is not None:
                ret_data = ret_data.decode(encoding = "utf-8")
            return ret_key, ret_data
        return None, None

    def seekNext(self):
        if not self.mIter.valid():
            return False
        self.mIter.next()
        return self.mIter.valid()

    def close(self):
        del self.mIter
