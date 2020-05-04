import re
from ._codec_data import _CodecData
#===============================================
class CodecNum(_CodecData):
    def __init__(self, master, parent, schema_instr, default_name):
        _CodecData.__init__(self, master, parent, schema_instr, default_name)
        self.mFormat = self._getProperty("format", "%.3e")
        self.mStatNoneCount = 0
        self.mStatValCount = 0
        self.mStatIntCount = 0
        self.mStatMinVal = None
        self.mStatMaxVal = None
        self._onDuty()

    def getType(self):
        return "num"

    def isAtomic(self):
        return True

    sEZeroPatternPlus = re.compile("[\\.]?0*e[+]?0*")
    sEZeroPatternMinus = re.compile("[\\.]?0*e-0*")

    def encode(self, value, encode_env):
        if value is None:
            self.mStatNoneCount += 1
            return "null"
        self.mStatValCount += 1
        if self.mStatMaxVal is None:
            self.mStatMinVal = self.mStatMaxVal = value
        else:
            if self.mStatMinVal < value:
                self.mStatMinVal = value
            if self.mStatMaxVal > value:
                self.mStatMaxVal = value
        if isinstance(value, int):
            self.mStatIntCount += 1
            return str(value)
        repr_val = self.sEZeroPatternPlus.sub("e", self.mFormat % value)
        repr_val = self.sEZeroPatternMinus.sub("e-", repr_val)
        if repr_val.endswith("e"):
            repr_val = repr_val[:-1]
        if repr_val.endswith("."):
            repr_val = repr_val[:-1]
        return repr_val

    def updateWStat(self, encode_env):
        self._updateProperty("stat", {
            "null": self.mStatNoneCount,
            "int": self.mStatIntCount,
            "val": self.mStatValCount,
            "min": self.mStatMinVal,
            "max": self.mStatMaxVal})

    def decode(self, int_obj, decode_env):
        return int_obj
