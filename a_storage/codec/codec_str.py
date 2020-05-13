from ._codec_data import _CodecData
#===============================================
class CodecStr(_CodecData):
    sGeneLetters = {
        "A": "0",
        "C": "1",
        "G": "2",
        "T": "3"
    }

    def __init__(self, master, parent, schema_instr, default_name):
        _CodecData.__init__(self, master, parent, schema_instr, default_name)
        self.mPreShift = 0
        self.mPreDict = None
        self.mDict = None
        self.mRepeatable = False

        opt = self._getProperty("opt", "")
        if opt == "dict":
            self.mDictList = self._getProperty("dictlist", [])
            self.mDict = {value: idx
                for idx, value in enumerate(self.mDictList)}
        else:
            if opt == "repeat":
                self.mRepeatable = True
            elif opt == "gene":
                self.mPreDict = self.sGeneLetters
                self.mPreShift = len(self.mPreDict)
                self.mPreBack = {int(val): str
                    for str, val in self.mPreDict.items()}
            else:
                self.mRepeatable = False
                assert not opt
        self.mStatNoneCount = 0
        self.mStatValCount = 0
        self.mStatMinL = None
        self.mStatMaxL = None
        self.mStatPre = 0
        self.getMaster().addRequirement("str")
        self._onDuty()

    def getType(self):
        if self.mDict is not None:
            return "str/dict"
        return "str"

    def isAtomic(self):
        return True

    def encode(self, value, encode_env):
        if value is None:
            self.mStatNoneCount += 1
            return "null"
        self.mStatValCount += 1
        if self.mPreShift > 0 and value in self.mPreDict:
            self.mStatPre += 1
            return self.mPreDict[value]
        v_len = len(value)
        if self.mStatMaxL is None:
            self.mStatMinL = self.mStatMaxL = v_len
        else:
            if self.mStatMinL < v_len:
                self.mStatMinL = v_len
            if self.mStatMaxL > v_len:
                self.mStatMaxL = v_len

        if self.mDict is not None:
            v_idx = self.mDict.get(value)
            if v_idx is None:
                v_idx = len(self.mDictList)
                self.mDictList.append(value)
                self.mDict[value] = v_idx
        else:
            v_idx = encode_env.addStr(value, self.mRepeatable)
        return str(v_idx + self.mPreShift)

    def updateWStat(self):
        stat_info = {
            "null": self.mStatNoneCount,
            "val": self.mStatValCount,
            "min-l": self.mStatMinL,
            "max-l": self.mStatMaxL}
        if self.mDict is not None:
            stat_info["dict-l"] = len(self.mDictList)
            self._updateProperty("dictlist", self.mDictList[:])
        if self.mPreShift > 0:
            stat_info["pre-val"] = self.mStatPre
        self._updateProperty("stat", stat_info)

    def decode(self, int_obj, decode_env):
        if int_obj is None:
            return None
        v_idx = int_obj
        if self.mPreShift > 0:
            if v_idx < self.mPreShift:
                return self.mPreBack[v_idx]
            v_idx -= self.mPreShift
        if self.mDict is not None:
            return self.mDictList[v_idx]
        return decode_env.getStr(v_idx)
