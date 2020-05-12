import os, json, random, logging

from codec import createDataCodec
from .a_io import AIOController
#========================================
class ASchema:
    def __init__(self, storage, name, dbname,
            schema_descr = None, write_mode = True):
        self.mStorage = storage
        self.mName = name
        self.mSchemaDescr = schema_descr
        self.mWriteMode = write_mode

        db_fpath = self.mStorage.getSchemaFilePath(dbname)
        if not os.path.exists(db_fpath):
            assert self.mWriteMode, (
                "Schema is empty for reading " + self.mName)
            os.mkdir(db_fpath)

        schema_fname = db_fpath + "/" + self.mName + ".json"
        if not self.mWriteMode:
            assert os.path.exists(schema_fname), (
                "Attempt to read from uninstalled database " + self.mName)
            with open(schema_fname, "r", encoding = "utf-8") as inp:
                self.mSchemaDescr = json.loads(inp.read())
        else:
            if os.path.exists(schema_fname):
                os.remove(schema_fname)

        self.mRequirements = set()
        self.mCodec = createDataCodec(self, None, self.mSchemaDescr["top"],
            self.mSchemaDescr["name"])
        self.mSchemaDescr["top"] = self.mCodec.getSchemaDescr()
        self.mFilters = self.mSchemaDescr.get("filter-list", dict())

        if self.mWriteMode:
            self.mTotal = 0
            self.mRH = random.Random(179)
            self.mSmpCount = self.mStorage.getSamplesCount()
            self.mSamples = []

        self.mIO = AIOController(self, dbname, self.mSchemaDescr["io"])
        self.mSchemaDescr["io"] = self.mIO.getDescr()

    def flush(self):
        self.mIO.flush()

    def close(self):
        self.mIO.flush()
        schema_fname = self.mStorage.getSchemaFilePath(
            self.mIO.getDbName()) + "/" + self.mName + ".json"
        if self.mWriteMode:
            self.careSamples()
        self.mIO.close()
        if self.mWriteMode:
            self.mSchemaDescr["total"] = self.mTotal
            self.mCodec.updateWStat()
            with open(schema_fname, "w", encoding = "utf-8") as outp:
                outp.write(json.dumps(self.mSchemaDescr, sort_keys = True,
                    indent = 4, ensure_ascii = False))

    def getStorage(self):
        return self.mStorage

    def getName(self):
        return self.mName

    def isWriteMode(self):
        return self.mWriteMode

    def getProperty(self, name):
        return self.mSchemaDescr.get(name)

    def getTotal(self):
        if self.mWriteMode:
            return self.mTotal
        return self.mSchemaDescr["total"]

    def addRequirement(self, rq):
        self.mRequirements.add(rq)

    def getFilteringProperties(self):
        return self.mFilters.keys()

    def useLastPos(self):
        return self.mSchemaDescr.get("use-last-pos", False)

    def getDBKeyType(self):
        return self.mIO.getDBKeyType()

    def isOptionRequired(self, opt):
        return opt in self.mRequirements

    def putRecord(self, key, record):
        self.mIO.putRecord(key, record, self.mCodec)
        self.mTotal += 1
        if len(self.mSamples) < self.mSmpCount:
            self.mSamples.append([key, record])
        else:
            idx = self.mRH.randrange(0, self.mTotal)
            if 1 <= idx < self.mSmpCount:
                self.mSamples[idx] = [key, record]

    def getRecord(self, key, filtering = None, last_pos = None):
        ret = self.mIO.getRecord(key, self.mCodec, last_pos)
        if ret is not None and filtering is not None:
            for key, value in filtering.items():
                field = self.mFilters.get(key)
                if field is None:
                    continue
                filtered = []
                for rec in ret:
                    if str(rec.get(field)) == value:
                        filtered.append(rec)
                ret = filtered
        return ret

    def careSamples(self):
        smp_fname = self.mStorage.getSchemaFilePath(
            self.mIO.getDbName()) + "/" + self.mName + ".samples"
        with open(smp_fname, "w", encoding = "utf-8") as outp:
            cnt_bad = 0
            for key, record in self.mSamples:
                repr0 = json.dumps(record,
                    sort_keys = True, ensure_ascii = False)
                rec1 = self.mIO.transformRecord(key, record, self.mCodec)
                repr1 = json.dumps(rec1,
                    sort_keys = True, ensure_ascii = False)
                rec2 = self.getRecord(key)
                repr2 = json.dumps(rec2,
                   sort_keys = True, ensure_ascii = False)
                if repr1 != repr2:
                    cnt_bad += 1
                report = {"_rep": True, "key": key, "ok": repr1 == repr2}
                print(json.dumps(report, sort_keys = True,
                    ensure_ascii = False), file = outp)
                if repr0 != repr1:
                    print(repr0, file = outp)
                print(repr1, file = outp)
                if repr2 != repr1:
                    print(repr2, file = outp)
        if cnt_bad == 0:
            logging.info("Samples check for %s: OK" % self.mName)
        else:
            logging.error("BAD! Samples check for %s: %d of %d"
                % (self.mName, cnt_bad, self.mSmpCount))
