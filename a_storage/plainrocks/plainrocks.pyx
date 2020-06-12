# distutils: language = c++
# distutils: libraries = rocksdb

from libcpp.string cimport string
from libcpp cimport bool
from plainrocks cimport PlainDbHandle

cdef class PyPlainRocks:
    cdef PlainDbHandle mH  # Hold a C++ instance which we're wrapping

    def __cinit__(self, options):
        self.mH = PlainDbHandle()
        for key, value in options:
            self.mH.setDBOption(bytes(key, encoding = "utf-8"), int(value))

    def regColumn(self, col_name, seek_support):
        return self.mH.regColumn(col_name, seek_support)
    
    def open(self, dbpath, write_mode, update_mode = False):
        self.mH.open(bytes(dbpath, encoding = "utf-8"), write_mode, update_mode)
        
    def close(self):
        self.mH.close()
        
    def put(self, col_idx, xkey, xvalue):
        self.mH.put(col_idx, xkey, xvalue)
        
    def get(self, col_idx, xkey):
        return self.mH.get(col_idx, xkey)
    
    def seek(self, xkey):
        return self.mH.seek(xkey)
    
    def seekNext(self):
        return self.mH.seekNext()
    
    def iteratorIsValid(self):
        return self.mH.iteratorIsValid()
    
    def curItKey(self):
        return self.mH.curItKey()
    
    def curItValue(self):
        return self.mH.curItValue()
