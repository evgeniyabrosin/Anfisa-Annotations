import json, random, logging

from .a_io import AIOController
from codec import getKeyCodec

#========================================
class AFastaSchema:
    def __init__(self, storage, name, dbname, schema_descr = None):
        self.mStorage = storage
        self.mName = name
        self.mWriteMode = schema_descr is not None

        self.mSchemaDescr = self.mStorage.preLoadSchemaData(
            dbname, self.mName, schema_descr)

        self.mTotals = self.mSchemaDescr.get("total", [])

        self.mIO = AIOController(self, dbname, self.mSchemaDescr["io"])
        self.mSchemaDescr["io"] = self.mIO.getDescr()

        self.mTypes = dict()
        for idx, type_name in enumerate(self.mSchemaDescr["types"]):
            tp_col_descr = self.mIO._regColumn("fasta", type_name)
            tp_conv = getKeyCodec(type_name)
            if len(self.mTotals) <= idx:
                self.mTotals.append(0)
            tp_h = _FastaTypeHandler(type_name, tp_col_descr, tp_conv, idx)
            self.mTypes[type_name] = tp_h
            if (self.mWriteMode and not self.mStorage.isDummyMode()):
                tp_h.initSamples(self.mStorage.getSamplesCount())

        self.mBlockSize = self.mSchemaDescr["block-size"]
        if self.mWriteMode:
            self.mTypesSet = set()
        logging.info("Fasta starts with types: %s"
            % ", ".join(sorted(self.mTypes.keys())))

    def flush(self):
        self.mIO.flush()
        self.keepSchema()

    def keepSchema(self):
        if not self.mWriteMode or self.mStorage.isDummyMode():
            return
        self.mSchemaDescr["total"] = self.mTotals
        self.mIO.updateWStat()
        self.mStorage.saveSchemaData(self.mIO.getDbName(),
            self.mName, self.mSchemaDescr)
        logging.info("Schema %s kept, total = %s"
            % (self.mName, ",".join(map(str, self.mTotals))))

    def close(self):
        self.mIO.flush()
        if self.mWriteMode:
            assert len(self.mTypesSet) == len(self.mTypes), (
                "No data loaded for "
                + " ".join(sorted(set(self.mTypes.keys()) - self.mTypesSet)))
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
        return self.mTotals

    def isOptionRequired(self, opt):
        return False

    def getFilteringProperties(self):
        return ["type"]

    def useLastPos(self):
        return True

    def getDBKeyType(self):
        return self.mIO.getDBKeyType()

    def loadReader(self, reader):
        hg_type = reader.getName()
        tp_h = self.mTypes[hg_type]
        self.mTypesSet.add(hg_type)
        cur_chrom, prev_diap = None, None
        for chrom, diap, letters in reader.readAll(self.mBlockSize):
            if chrom != cur_chrom:
                assert diap[0] == 1
                cur_chrom = chrom
                prev_diap = None
            elif prev_diap is not None:
                assert prev_diap[1] == diap[0]
            assert (diap[0] - 1) % self.mBlockSize == 0
            self.mIO._putColumns(tp_h.encodeKey("chr" + chrom, diap[0] - 1),
                [letters], col_seq = tp_h.getColSeq())
            self.mTotals[tp_h.getIdx()] += 1
            tp_h.addSample(chrom, diap, letters)
            prev_diap = diap

    def getRecord(self, key, filtering, last_pos = None):
        if (filtering is None or "type" not in filtering
                or filtering["type"] not in self.mTypes):
            raise Exception("Request requires 'type' argument = %s"
                % sorted(self.mTypes.keys()))

        tp_h = self.mTypes[filtering["type"]]
        chrom, pos = key
        base_pos = (pos - 1) - ((pos - 1) % self.mBlockSize)
        if not chrom.startswith("chr"):
            chrom = "chr" + chrom

        seq_l = self.mIO._getColumns(
            tp_h.encodeKey(chrom, base_pos), col_seq = tp_h.getColSeq())

        loc_pos = pos - 1 - base_pos
        letters = seq_l[0]
        if letters is None or len(letters) < loc_pos:
            return None
        if last_pos is None:
            return letters[loc_pos]
        loc_last = last_pos - base_pos - 1
        if loc_last + 1 < len(letters):
            return letters[loc_pos:loc_last + 1]
        ret = [letters[loc_pos:]]
        while len(letters) == self.mBlockSize:
            base_pos += self.mBlockSize
            loc_last -= self.mBlockSize
            seq_l = self.mIO._getColumns(tp_h.encodeKey(chrom, base_pos),
                col_seq = tp_h.getColSeq())
            letters = seq_l[0]
            if letters is None:
                break
            if loc_last + 1 < len(letters):
                ret.append(letters[:loc_last + 1])
                break
            ret.append(letters)
        return ''.join(ret)

    def careSamples(self):
        if not self.mWriteMode or self.mStorage.isDummyMode():
            return
        with self.mStorage.openSchemaSamplesFile(
                self.mIO.getDbName(), self.mName) as outp:
            for tp_h in self.mTypes.values():
                cnt_bad = 0
                for chrom, diap, letters in tp_h.iterSamples():
                    letters1 = self.getRecord((chrom, diap[0]),
                        {"type": tp_h.getName()}, diap[1])
                    if letters != letters1:
                        cnt_bad += 1
                    print(json.dumps({
                        "ok": letters != letters1,
                        "tp": tp_h.getName(),
                        "chrom": chrom,
                        "diap": diap,
                        "letters": letters,
                        "db-letters": letters1}), file = outp)
                if cnt_bad == 0:
                    logging.info("Samples check for %s/%s: OK"
                        % (self.mName, tp_h.getName()))
                else:
                    logging.error("BAD! Samples check for %s/%s: %d of %d"
                        % (self.mName, tp_h.getName(),
                        cnt_bad, tp_h.getSmpCount()))

#========================================
class _FastaTypeHandler:
    def __init__(self, name, tp_col_descr, tp_conv, idx):
        self.mName = name
        self.mColDescr = tp_col_descr
        self.mHgConv = tp_conv
        self.mIdx = idx
        self.mSmpCount = None
        self.mTotal = None
        self.mSamples = None
        self.mRH = None
        self.mPrevIdx = None

    def getName(self):
        return self.mName

    def getIdx(self):
        return self.mIdx

    def encodeKey(self, chrom, pos):
        return self.mHgConv.encode(chrom, pos)

    def getColSeq(self):
        return [self.mColDescr]

    def initSamples(self, smp_count):
        self.mRH = random.Random(179)
        self.mSmpCount = smp_count
        self.mSamples = []
        self.mTotal = 0

    def addSample(self, chrom, diap, letters):
        if self.mPrevIdx is not None:
            if self.mSamples[self.mPrevIdx][0] == chrom:
                add_letters = letters[:5]
                self.mSamples[self.mPrevIdx][1][1] += len(add_letters)
                self.mSamples[self.mPrevIdx][2] += add_letters
                self.mPrevIdx = None
        self.mTotal += 1
        if self.mTotal <= self.mSmpCount:
            idx = len(self.mSamples)
            self.mSamples.append(None)
        else:
            idx = self.mRH.randrange(0, self.mTotal - 1)
            if idx >= self.mSmpCount:
                return
        if self.mRH.choice([True, True, False]):
            pos = self.mRH.randrange(0, len(letters) - 1)
        else:
            pos = max(0, len(letters) - 3)
        test_letters = letters[pos:pos + 5]
        self.mSamples[idx] = [chrom,
            [diap[0] + pos, diap[0] + pos + len(test_letters) - 1],
            test_letters]
        if len(test_letters) < 5:
            self.mPrevIdx = idx

    def iterSamples(self):
        return iter(self.mSamples)

    def getSmpCount(self):
        return len(self.mSamples)
