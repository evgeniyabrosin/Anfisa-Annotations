import json, random, logging
from datetime import datetime, timedelta

from forome_tools.log_err import logException
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
        self.mStorage.saveSchemaData(self)
        logging.info("Schema %s kept, total = %d" % (self.mName, self.mTotal))

    def close(self):
        self.mIO.flush()
        self.keepSchema()
        self.keepSamples()
        if (self.mWriteMode
                and not (self.mUpdateMode or self.mStorage.isDummyMode())):
            self.checkSamples()

    def _addDirectSample(self, key, record):
        self.mSamples.append([key, record])

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

    def getIO(self):
        return self.mIO

    def getSchemaDescr(self):
        return self.mSchemaDescr

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

    def keepSamples(self):
        if (not self.mWriteMode or self.mUpdateMode
                or self.mStorage.isDummyMode()):
            return
        ready_samples = []
        with open(self.mStorage.getSchemaFilePath(self, "0.samples"),
                "w", encoding = "utf-8") as output:
            for idx, smp_info in enumerate(self.mSamples):
                key, full_record = smp_info
                record = self.getIO().normalizeSample(key, full_record)
                ready_samples.append([key, record])
                print(json.dumps({
                    "no": idx + 1,
                    "key": key}, ensure_ascii = False), file = output)
                print(json.dumps(record, ensure_ascii = False), file = output)
        with open(self.mStorage.getSchemaFilePath(self, "1.samples"),
                "w", encoding = "utf-8") as output:
            for idx, smp_info in enumerate(ready_samples):
                key, record0 = smp_info
                record1 = self.mIO.transformRecord(key, record0, self.mCodec)
                presentations = [json.dumps(rec,
                    sort_keys = True, ensure_ascii = False)
                    for rec in (record0, record1)]
                print(json.dumps({
                    "no": idx + 1,
                    "same-orig": presentations[0] == presentations[1],
                    "key": key}, ensure_ascii = False), file = output)
                print(presentations[1], file = output)

    def checkSamples(self, output_stream = None):
        if not self.getIO().properAccess():
            return
        smp_input = open(self.mStorage.getSchemaFilePath(self, "1.samples"),
            "r", encoding = "utf-8")
        cnt_ok, cnt_bad, cnt_fail = 0, 0, 0
        if output_stream is None:
            output = open(self.mStorage.getSchemaFilePath(self, "2.samples"),
                "w", encoding = "utf-8")
        else:
            output = output_stream
        while True:
            try:
                line_rep = smp_input.readline()
                if not line_rep:
                    break
                rep_obj = json.loads(line_rep)
                record1 = json.loads(smp_input.readline())
                record2 = self.getRecord(tuple(rep_obj["key"]))
                presentations = [json.dumps(rec,
                    sort_keys = True, ensure_ascii = False)
                    for rec in (record1, record2)]
                rep_obj["ok"] = (presentations[0] == presentations[1])
                print(json.dumps(rep_obj, sort_keys = True), file = output)
                if rep_obj["ok"]:
                    cnt_ok += 1
                else:
                    cnt_bad += 1
                    print(presentations[1], file = output)
            except Exception:
                msg_txt = logException("Check samples")
                cnt_fail += 1
                print(json.dumps({"exception": msg_txt},
                    ensure_ascii = False), file = output)
        print(json.dumps({"tp": "result",
            "ok": cnt_ok, "bad": cnt_bad, "fail": cnt_fail}),
            file = output)
        smp_input.close()
        if output_stream is None:
            output.close()

        if cnt_bad + cnt_fail == 0:
            logging.info("Samples check(%d) for %s: OK" % (cnt_ok, self.mName))
        else:
            logging.error("BAD! Samples check for %s: %d of %d (+ %d failures)"
                % (self.mName, cnt_bad, cnt_bad + cnt_ok, cnt_fail))

    def locateChrom(self, chrom):
        with self.getIO().seekIt((chrom, 1)) as iter_h:
            it_key, _ = iter_h.getCurrent()
            return it_key and it_key[0] == chrom
