#include <iostream>

#include "plainrocks.h"

using namespace plainrocks;

PlainDbHandle::PlainDbHandle(): 
        mDB(NULL), mSeekColumnIdx(-1), mIterator(NULL), mWriteMode(false) {
    mReadOptions.fill_cache = false;
    mColumnDescriptors.push_back(rocksdb::ColumnFamilyDescriptor(
        rocksdb::kDefaultColumnFamilyName, rocksdb::ColumnFamilyOptions()));            
}

PlainDbHandle::~PlainDbHandle() {
    if (mDB)
        close();
}

void PlainDbHandle::close() {
    if (mIterator) {
        delete mIterator;
        mIterator = NULL;
    }
    if (mWriteMode && mDB) {
        for (auto col_h: mColumnHandlers) {
            mDB->CompactRange(mCompactRangeOptions, col_h, NULL, NULL);
        }
    }
    if (mDB) {
        for (auto col_h : mColumnHandlers) {
            mDB->DestroyColumnFamilyHandle(col_h);
        }
        CancelAllBackgroundWork(mDB);
        delete mDB;
    } else 
    {
        for (auto col_h: mColumnHandlers) {
            delete col_h;
        }
    }
}

void PlainDbHandle::setDBOption(const std::string name, int value) {
    if (name == "create_if_missing") {
        mDbOptions.create_if_missing = value;
        return;
    }
    if (name == "error_if_exists") {
        mDbOptions.error_if_exists = value;
        return;
    }
    if (name == "paranoid_checks") {
        mDbOptions.paranoid_checks = value;
        return;
    }
    if (name == "max_open_files") {
        mDbOptions.max_open_files = value;
        return;
    }
    if (name == "max_file_opening_threads") {
        mDbOptions.max_file_opening_threads = value;
        return;
    }
    if (name == "write_buffer_size") {
        mDbOptions.write_buffer_size = value;
        return;
    }
    if (name == "max_write_buffer_number") {
        mDbOptions.max_write_buffer_number = value;
        return;
    }
    if (name == "target_file_size_base") {
        mDbOptions.target_file_size_base = value;
        return;
    }
    if (name == "max_total_wal_size") {
        mDbOptions.max_total_wal_size = value;
        return;
    }
    if (name == "use_fsync") {
        mDbOptions.use_fsync = value;
        return;
    }
    throw std::invalid_argument("unknown db option");
}    

int PlainDbHandle::regColumn(const std::string col_name, bool seek_support) {
    int ret_idx = mColumnDescriptors.size();
    ColumnFamilyOptions col_options;
    col_options.compression = rocksdb::CompressionType::kNoCompression;
    mColumnDescriptors.push_back(ColumnFamilyDescriptor(col_name, col_options));
    if (seek_support)
        mSeekColumnIdx = ret_idx;
    return ret_idx;
}

void PlainDbHandle::open(const std::string dbpath, 
        bool write_mode, bool update_mode) {
    if (write_mode) {
        if (!update_mode) {
            rocksdb::DB* db;
            std::vector<rocksdb::ColumnFamilyHandle*> handlers;
            
            DB::Open(mDbOptions, dbpath, &db);
            for (unsigned idx = 1; idx < mColumnDescriptors.size(); idx++) {
                rocksdb::ColumnFamilyHandle* cf;
                db->CreateColumnFamily(
                    mColumnDescriptors[idx].options, 
                    mColumnDescriptors[idx].name, &cf);
                handlers.push_back(cf);
            }
            for (auto col_h : handlers) {
                db->DestroyColumnFamilyHandle(col_h);
            }
            delete db;
        }
        DB::Open(mDbOptions, dbpath, 
            mColumnDescriptors, &mColumnHandlers, &mDB);
        mWriteMode = true;
    } else {
        DB::OpenForReadOnly(mDbOptions, dbpath, 
            mColumnDescriptors, &mColumnHandlers, &mDB);
    }
}

void PlainDbHandle::put(int col_idx, 
        const std::string xkey, const std::string xvalue) {
    rocksdb::Status status = mDB->Put(mWriteOptions, mColumnHandlers[col_idx], xkey, xvalue);
}

std::string PlainDbHandle::get(int col_idx, const std::string xkey){
    std::string xvalue;
    rocksdb::Status status = mDB->Get(
        mReadOptions, mColumnHandlers[col_idx], xkey, &xvalue);
    if (!status.ok())
        xvalue = "";
    return xvalue;
}

bool PlainDbHandle::seek(const std::string xkey) {
    if (mIterator == NULL) {
        if (mSeekColumnIdx < 0) {
            throw std::invalid_argument("No seek column");
        }
        mIterator = mDB->NewIterator(mReadOptions, mColumnHandlers[mSeekColumnIdx]);
    }
    mIterator->Seek(rocksdb::Slice(xkey));
    return mIterator->Valid();
}

bool PlainDbHandle::seekNext() {
    if (mIterator && mIterator->Valid())
        mIterator->Next();
    return mIterator->Valid();
}

bool PlainDbHandle::iteratorIsValid() {
    return mIterator && mIterator->Valid();
}

std::string PlainDbHandle::curItKey() {
    return mIterator->key().ToString();
}

std::string PlainDbHandle::curItValue() {
    return mIterator->value().ToString();
}

