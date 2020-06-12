from libcpp.string cimport string
from libcpp cimport bool

cdef extern from "plainrocks.c":
    pass

cdef extern from "plainrocks.h" namespace "plainrocks":
    cdef cppclass PlainDbHandle:
        PlainDbHandle() except +
        void setDBOption(const string name, int value)
        int regColumn(const string col_name, bool seek_support)
        void open(const string dbpath, bool write_mode, bool update_mode)
        void close()
        void put(int col_idx, const string xkey, const string xvalue)
        string get(int col_idx, const string xkey)            
        bool seek(const string xkey)
        bool seekNext()            
        bool iteratorIsValid()
        string curItKey()
        string curItValue()        
