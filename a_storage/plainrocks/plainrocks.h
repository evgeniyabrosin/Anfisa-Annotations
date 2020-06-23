#ifndef PLAINROCKS_H
#define PLAINROCKS_H

#include <iostream>
#include <fstream>

#include "rocksdb/db.h"
#include "rocksdb/slice.h"
#include "rocksdb/options.h"
#include "rocksdb/convenience.h"

using namespace rocksdb;

namespace plainrocks {
    class PlainDbHandle {
        protected:
            rocksdb::Options   mDbOptions;
            rocksdb::DB*       mDB;
            std::vector<rocksdb::ColumnFamilyDescriptor> mColumnDescriptors;
            int mSeekColumnIdx;
            rocksdb::Iterator* mIterator;
            bool mWriteMode;
            std::vector<rocksdb::ColumnFamilyHandle*> mColumnHandlers;
            std::ofstream* mLog;
            
            rocksdb::ReadOptions mReadOptions;
            rocksdb::WriteOptions mWriteOptions;
            rocksdb::CompactRangeOptions mCompactRangeOptions;
        
        public:
            PlainDbHandle();
            ~PlainDbHandle();
            
            void setDBOption(const std::string name, int value);
            
            void setLog(const std::string fpath);
            
            int regColumn(const std::string col_name, bool seek_support);
            
            void open(const std::string dbpath, bool write_mode, bool update_mode);
            
            void close();
            
            void put(int col_idx, const std::string xkey, const std::string xvalue);
            
            std::string get(int col_idx, const std::string xkey);
            
            bool seek(const std::string xkey);
            
            bool seekNext();
            
            bool iteratorIsValid();
            
            std::string curItKey();
            
            std::string curItValue();
    };
}

#endif
