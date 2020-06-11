import logging, json
from threading import Condition
import pyrocksdb as rocksdb

from .deep_comp import DeepCompWriter
#========================================
class AConnector:
    def __init__(self, storage, name, write_mode = False):
        self.mStorage = storage
        self.mName = name
        self.mRefCount = 0
        self.mFilePath = self.mStorage.getDBFilePath(name)
        self.mWriteMode = write_mode
        self.mColIndex = dict()
        self.mSeekIterator = ASeekIterator(self)

        self.mColDescriptors = rocksdb.VectorColumnFamilyDescriptor()
        self.mColDescriptors.append(rocksdb.ColumnFamilyDescriptor
            (rocksdb.DefaultColumnFamilyName, rocksdb.ColumnFamilyOptions()))
        self.mRdOpts = rocksdb.ReadOptions()
        self.mWrOpts = rocksdb.WriteOptions() if self.mWriteMode else None
        self.mDeepWriter = None
        self.mDB = None
        self.mColHandlers = None
        if storage.isDummyMode():
            logging.info("Attention: DB %s runs in dummy mode" % self.mName)
        elif storage.isDeepCompMode():
            assert self.mWriteMode
            self.mStorage.newDB(self.mName)
            self.mDeepWriter = DeepCompWriter(self.mFilePath + "/data.bin")
        else:
            self.mDB = rocksdb.DB()
            if self.mWriteMode:
                self.mStorage.newDB(self.mName)
                self.mDB.open(self._dbOptions(), self.mFilePath)

    def _incRefCount(self):
        self.mRefCount += 1

    def _decRefCount(self):
        self.mRefCount -= 1
        return self.mRefCount

    def _getColH(self, col_name):
        return self.mColHandlers[self.mColIndex[col_name]]

    def getName(self):
        return self.mName

    def properAccess(self):
        return self.mDB is not None

    def getSeekColName(self):
        return self.mSeekIterator.getColName()

    def _dbOptions(self):
        options = rocksdb.Options()
        for key, val in self.mStorage.getDbOptions():
            setattr(options, key, val)
        return options

    def _colOptions(self, col_attrs):
        col_options = rocksdb.ColumnFamilyOptions()
        col_options.OptimizeLevelStyleCompaction()
        col_options.set_compression("no_compression")
        for key, val in col_attrs.items():
            if key.startswith('-'):
                continue
            if key == "compression":
                col_options.set_compression(val)
            else:
                setattr(col_options, key, val)
        return col_options

    def regColumn(self, c_name, col_attrs, seek_column = False):
        assert self.mColHandlers is None
        col_name = bytes(c_name, encoding = "utf-8")
        assert col_name not in self.mColIndex
        self.mColIndex[col_name] = len(self.mColDescriptors)
        self.mColDescriptors.append(rocksdb.ColumnFamilyDescriptor(
            col_name, self._colOptions(col_attrs)))
        if self.mWriteMode and self.mDB is not None:
            s, cf = self.mDB.create_column_family(
                self._colOptions(col_attrs), col_name)
            del cf
        if seek_column:
            self.regSeekColumn(col_name)
        return col_name

    def regSeekColumn(self, col_name):
        assert self.mSeekIterator.getColName() is None, (
            "Duplication of seek columns: %s/%s"
            % (self.mSeekIterator.getColName(), col_name))
        assert col_name in self.mColIndex, (
            "Seek column %s is not registered" % col_name)
        self.mSeekIterator.setColName(col_name)

    def activate(self):
        assert self.mColHandlers is None
        if self.mDB is None:
            return
        if self.mWriteMode:
            self.mDB.close()
            _, self.mColHandlers = self.mDB.open(
                self._dbOptions(), self.mFilePath, self.mColDescriptors)
        else:
            _, self.mColHandlers = self.mDB.open_for_readonly(
                self._dbOptions(), self.mFilePath, self.mColDescriptors)
        self.mSeekIterator.activate()
        # self._reportKeys()

    def close(self):
        if self.mDeepWriter is not None:
            self.mDeepWriter.close()
            self.mDeepWriter = None
        if self.mDB is None:
            return
        self.mSeekIterator.close()
        # self._reportKeys()
        if self.mWriteMode:
            compact_opt = rocksdb.CompactRangeOptions()
            for col_h in self.mColHandlers:
                self.mDB.compact_range(compact_opt, col_h, None, None)
        for col_h in self.mColHandlers:
            del col_h
        self.mDB.close()

    def isWriteMode(self):
        return self.mWriteMode

    def getColumnCount(self):
        return len(self.mColNames)

    def _reportKeys(self):
        if self.mDB is None:
            return
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

    def putData(self, xkey, column_h, data, use_encode = True):
        assert self.mWriteMode
        if self.mDeepWriter is not None:
            self.mDeepWriter.put(xkey, column_h, column_h.encode(data))
            return
        if self.mDB is None:
            return
        if use_encode:
            data = column_h.encode(data)
        if len(data) > 0:
            col_h = self._getColH(column_h.getName())
            self.mDB.put(self.mWrOpts, col_h, xkey, data)

    def getData(self, xkey, column_h):
        if self.mDB is None:
            return None
        col_h = self._getColH(column_h.getName())
        blob = self.mDB.get(self.mRdOpts, col_h, xkey)
        if not blob.status.ok():
            return None
        return column_h.decode(blob.data)

    def seekIt(self, xkey):
        return self.mSeekIterator.attach(xkey)

#========================================
class ASeekIterator:
    def __init__(self, master):
        self.mMaster = master
        self.mIter = False
        self.mColName = None
        self.mColH = None
        self.mCondition = None
        self.mInUse = False

    def getColName(self):
        return self.mColName

    def setColName(self, col_name):
        self.mColName = col_name

    def activate(self):
        if self.mColName is None:
            self.mIter = None
        else:
            self.mColH = self.mMaster._getColH(self.mColName)
            self.mCondition = Condition()

    def getCurrent(self):
        assert self.mIter is not False
        if self.mIter is not None and self.mIter.valid():
            return self.mIter.key(), self.mIter.value()
        return None, None

    def attach(self, xkey):
        if self.mIter is None:
            return self
        while True:
            with self.mCondition:
                if self.mIter is False:
                    assert self.mColName is not None
                    if not self.mMaster.properAccess():
                        self.mIter = None
                        return self
                    self.mIter = self.mMaster.mDB.iterator(
                        self.mMaster.mRdOpts, self.mColH)
                if self.mInUse:
                    self.mCondition.wait()
                else:
                    self.mInUse = True
                    self.mIter.seek(xkey)
                    return self

    def detach(self):
        if self.mIter is not None:
            with self.mCondition:
                self.mCondition.notify()
                self.mInUse = False

    def seekNext(self):
        if self.mIter is None or not self.mIter.valid():
            return False
        self.mIter.next()
        return self.mIter.valid()

    def close(self):
        if self.mIter not in (None, False):
            del self.mIter
        self.mIter = None
