from ._codec import _Codec
#===============================================
class CodecStr(_Codec):
    def __init__(self, master, parent, schema_instr, default_name):
        _Codec.__init__(self, master, parent, schema_instr, default_name)
        opt = self._getProperty("opt")
        if opt == "dict":
            self.mDictList = self._getProperty("dictlist", [])
            self.mDict = {value: idx
                for idx, value in enumerate(self.mDictList)}
        else:
            self.mDict = None
            assert opt is None
        self.mStatNoneCount = 0
        self.mStatValCount = 0
        self.mStatMinL = None
        self.mStatMaxL = None
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
            v_idx = encode_env.addStr(value)
        return str(v_idx)

    def updateWStat(self, encode_env):
        stat_info = {
            "null": self.mStatNoneCount,
            "val": self.mStatValCount,
            "min-l": self.mStatMinL,
            "max-l": self.mStatMaxL}
        if self.mDict is not None:
            stat_info["dict-l"] = len(self.mDictList)
            self._updateProperty("dictlist", self.mDictList[:])
        self._updateProperty("stat", stat_info)

    def decode(self, int_obj, decode_env):
        if int_obj is None:
            return None
        v_idx = int_obj
        if self.mDict is not None:
            return self.mDictList[v_idx]
        return decode_env.getStr(v_idx)
