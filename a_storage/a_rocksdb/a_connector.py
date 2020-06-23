import logging
from threading import Condition
from plainrocks import PyPlainRocks

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

        self.mDeepWriter = None
        self.mDB = None
        if storage.isDummyMode():
            logging.info("Attention: DB %s runs in dummy mode" % self.mName)
        elif storage.isDeepCompMode():
            assert self.mWriteMode
            self.mStorage.newDB(self.mName)
            self.mDeepWriter = DeepCompWriter(self.mFilePath + "/data.bin")
        else:
            self.mDB = PyPlainRocks(self.mStorage.getDbOptions())
            if self.mWriteMode:
                self.mStorage.newDB(self.mName)

    def _incRefCount(self):
        self.mRefCount += 1

    def _decRefCount(self):
        self.mRefCount -= 1
        return self.mRefCount

    def _getDB(self):
        return self.mDB

    def getName(self):
        return self.mName

    def properAccess(self):
        return self.mDB is not None

    def getSeekColName(self):
        return self.mSeekIterator.getColName()

    def regColumn(self, c_name, col_attrs, seek_column = False):
        col_name = bytes(c_name, encoding = "utf-8")
        assert col_name not in self.mColIndex
        if self.mDB is not None:
            self.mColIndex[col_name] = self.mDB.regColumn(
                col_name, seek_column)
        else:
            self.mColIndex[col_name] = len(self.mColIndex)
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
        if self.mDB is not None:
            self.mDB.open(self.mFilePath, self.mWriteMode)
        self.mSeekIterator.activate()

    def close(self):
        if self.mDeepWriter is not None:
            self.mDeepWriter.close()
            self.mDeepWriter = None
        if self.mDB is None:
            return
        self.mDB.close()

    def isWriteMode(self):
        return self.mWriteMode

    def getColumnCount(self):
        return len(self.mColIndex)

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
            col_idx = self.mColIndex[column_h.getName()]
            self.mDB.put(col_idx, xkey, data)

    def getData(self, xkey, column_h):
        if self.mDB is None:
            return None
        col_idx = self.mColIndex[column_h.getName()]
        xdata = self.mDB.get(col_idx, xkey)
        if len(xdata) == 0:
            return None
        return column_h.decode(xdata)

    def seekIt(self, xkey):
        return self.mSeekIterator.attach(xkey)

#========================================
class ASeekIterator:
    def __init__(self, master):
        self.mMaster = master
        self.mColName = None
        self.mCondition = None
        self.mDB = None
        self.mInUse = False

    def getColName(self):
        return self.mColName

    def setColName(self, col_name):
        self.mColName = col_name

    def activate(self):
        if self.mColName is not None:
            self.mDB = self.mMaster._getDB()
            if self.mDB is not None:
                self.mCondition = Condition()

    def getCurrent(self):
        if self.mDB is not None and self.mDB.iteratorIsValid():
            return self.mDB.curItKey(), self.mDB.curItValue()
        return None, None

    def attach(self, xkey):
        if self.mDB is None:
            return self
        while True:
            with self.mCondition:
                if self.mInUse:
                    self.mCondition.wait()
                else:
                    self.mInUse = True
                    self.mDB.seek(xkey)
                    return self

    def detach(self):
        if self.mDB is not None:
            with self.mCondition:
                self.mCondition.notify()
                self.mInUse = False

    def seekNext(self):
        if self.mDB is None:
            return False
        return self.mDB.seekNext()
