import os, json, random, logging

from codec import createCodec
#========================================
class ASchema:
    def __init__(self, storage, name, dbname,
            schema_descr = None, write_mode = True):
        self.mStorage = storage
        self.mName = name
        self.mSchemaDescr = schema_descr
        self.mWriteMode = write_mode
        if self.mWriteMode:
            self.mTotal = 0
            self.mRH = random.Random(179)
            self.mSmpCount = self.mStorage.getSamplesCount()
            self.mSamples = []

        schema_fname = self.mStorage.getSchemaFilePath(self.mName + ".json")
        if not self.mWriteMode:
            assert os.path.exists(schema_fname), (
                "Attempt to read from uninstalled database " + self.mName)
            with open(schema_fname, "r", encoding = "utf-8") as inp:
                self.mSchemaDescr = json.loads(inp.read())
        else:
            if os.path.exists(schema_fname):
                os.remove(schema_fname)

        self.mRequirements = set()
        self.mCodec = createCodec(self, None, self.mSchemaDescr["top"],
            self.mSchemaDescr["name"])
        self.mSchemaDescr["top"] = self.mCodec.getSchemaDescr()
        self.mFilters = self.mSchemaDescr.get("filter-list", dict())

        self.mDbConnector = self.mStorage.openConnection(
            dbname, self.mSchemaDescr["key"], self.mWriteMode)
        self.mColNames = [self.mDbConnector._regColumn(
            self.mName + "_base", "base")]
        if "str" in self.mRequirements:
            self.mColNames.append(self.mDbConnector._regColumn(
                self.mName + "_str", "str"))

    def close(self):
        schema_fname = self.mStorage.getSchemaFilePath(self.mName + ".json")
        with open(schema_fname, "w", encoding = "utf-8") as outp:
            outp.write(json.dumps(self.mSchemaDescr, sort_keys = True,
                indent = 4, ensure_ascii = False))
        if self.mWriteMode:
            self.careSamples()
        self.mStorage.closeConnection(self.mDbConnector)

    def getName(self):
        return self.mName

    def getProperty(self, name):
        return self.mSchemaDescr.get(name)

    def addRequirement(self, rq):
        self.mRequirements.add(rq)

    def getFilteringProperties(self):
        return self.mFilters.keys()

    def getDBKeyType(self):
        return self.mDbConnector.getKeyType()

    def putRecord(self, key, record):
        encode_env = AEncodeEnv()
        data_seq = [self.mCodec.encode(record, encode_env)]
        if len(self.mColNames) > 1:
            data_seq.append(encode_env.getBuf())
        self.mDbConnector.putData(key, self.mColNames, data_seq)
        self.mTotal += 1
        if len(self.mSamples) < self.mSmpCount:
            self.mSamples.append([key, record])
        else:
            idx = self.mRH.randrange(0, self.mTotal)
            if idx < self.mSmpCount:
                self.mSamples[idx] = [key, record]

    def getRecord(self, key, filtering = None):
        data_seq = self.mDbConnector.getData(key, self.mColNames)
        if data_seq[0] is None:
            return None
        decode_env = ADecodeEnv(data_seq[1] if len(data_seq) > 1 else None)
        ret = self.mCodec.decode(json.loads(data_seq[0]), decode_env)
        if filtering is not None:
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
            self.mName + ".samples")
        with open(smp_fname, "w", encoding = "utf-8") as outp:
            cnt_bad = 0
            for key, record in self.mSamples:
                repr0 = json.dumps(record,
                    sort_keys = True, ensure_ascii = False)
                encode_env = AEncodeEnv()
                int_rec = self.mCodec.encode(record, encode_env)
                decode_env = ADecodeEnv(encode_env.getBuf())
                rec1 = self.mCodec.decode(json.loads(int_rec), decode_env)
                rec2 = self.getRecord(key)
                repr1 = json.dumps(rec1,
                    sort_keys = True, ensure_ascii = False)
                repr2 = json.dumps(rec2,
                   sort_keys = True, ensure_ascii = False)
                if repr1 != repr2:
                    cnt_bad += 1
                report = {"_rep": True, "key": key, "ok": repr1 == repr2,
                    "xkey": self.mDbConnector.getXKey(key).hex()}
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

#========================================
class AEncodeEnv:
    def __init__(self):
        self.mStrSeq = []

    def addStr(self, txt):
        ret = len(self.mStrSeq)
        self.mStrSeq.append(txt)
        return ret

    def getBuf(self):
        return '\0'.join(self.mStrSeq)

#========================================
class ADecodeEnv:
    def __init__(self, str_buf = None):
        self.mStrSeq = str_buf.split('\0') if str_buf is not None else []

    def getStr(self, idx):
        return self.mStrSeq[idx]
