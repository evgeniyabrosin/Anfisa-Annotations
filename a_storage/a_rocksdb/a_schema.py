import json, random, logging
from datetime import datetime, timedelta

from codec import createDataCodec
from .a_io import AIOController
#========================================
class ASchema:
    def __init__(self, storage, name, dbname,
            schema_descr = None, update_mode = False):
        self.mStorage = storage
        self.mName = name
        self.mWriteMode = schema_descr is not None
        self.mUpdateMode = update_mode
        assert not self.mUpdateMode or self.mWriteMode

        self.mSchemaDescr = self.mStorage.preLoadSchemaData(
            dbname, self.mName, schema_descr, self.mUpdateMode)

        self.mRequirements = {"base"}
        self.mCodecDict = dict()
        self.mCodec = createDataCodec(self, None, self.mSchemaDescr["top"],
            self.mSchemaDescr["name"])
        self.mSchemaDescr["top"] = self.mCodec.getSchemaDescr()
        self.mFilters = self.mSchemaDescr.get("filter-list", dict())
        self.mTotal = self.mSchemaDescr.get("total", 0)

        if self.mWriteMode:
            self.mNextKeepTime = None

        if (self.mWriteMode and not self.mUpdateMode
                and not self.mStorage.isDummyMode()):
            self.mNextKeepTime = None
            self.mRH = random.Random(179)
            self.mSmpCount = self.mStorage.getSamplesCount()
            self.mSamples = []
        else:
            self.mSamples = None

        self.mIO = AIOController(self, dbname, self.mSchemaDescr["io"])
        self.mSchemaDescr["io"] = self.mIO.getDescr()

    def flush(self):
        self.mIO.flush()
        self.keepSchema()

    def keepSchema(self):
        if (not self.mWriteMode or self.mUpdateMode
                or self.mStorage.isDummyMode()):
            return
        self.mSchemaDescr["total"] = self.mTotal
        self.mCodec.updateWStat()
        self.mIO.updateWStat()
        self.mStorage.saveSchemaData(self.mIO.getDbName(),
            self.mName, self.mSchemaDescr)
        logging.info("Schema %s kept, total = %d" % (self.mName, self.mTotal))

    def close(self):
        self.mIO.flush()
        self.keepSchema()
        self.careSamples()

    def getStorage(self):
        return self.mStorage

    def getName(self):
        return self.mName

    def isWriteMode(self):
        return self.mWriteMode

    def getProperty(self, name):
        return self.mSchemaDescr.get(name)

    def getTotal(self):
        return self.mTotal

    def addRequirement(self, rq):
        self.mRequirements.add(rq)

    def setCodecByLabel(self, label, codec):
        assert label not in self.mCodecDict
        self.mCodecDict[label] = codec

    def getCodecByLabel(self, label):
        return self.mCodecDict.get(label)

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
        time_now = datetime.now()
        if (self.mNextKeepTime is not None
                and time_now >= self.mNextKeepTime):
            self.keepSchema()
            self.mNextKeepTime = None
        if self.mNextKeepTime is None:
            self.mNextKeepTime = (time_now
                + timedelta(seconds = self.mStorage.getLoadKeepSchemaSec()))

        if self.mSamples is not None:
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
        if (not self.mWriteMode or self.mUpdateMode
                or self.mStorage.isDummyMode()):
            return
        with self.mStorage.openSchemaSamplesFile(
                self.mIO.getDbName(), self.mName) as outp:
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
