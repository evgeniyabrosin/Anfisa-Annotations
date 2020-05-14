import re
from ._codec_data import _CodecData
#===============================================
class CodecNum(_CodecData):
    def __init__(self, master, parent, schema_instr, default_name):
        _CodecData.__init__(self, master, parent, schema_instr, default_name)
        self.mFormat = self._getProperty("format", "%.3e")
        self.mStatDetails = not self.getMaster().getProperty("no-stat-details")
        stat_info = self._getProperty("stat", dict())
        self.mStatNoneCount = stat_info.get("null", 0)
        self.mStatIntCount = stat_info.get("int", 0)
        if self.mStatDetails:
            self.mStatValCount = stat_info.get("val", 0)
            self.mStatMinVal = stat_info.get("min", 0)
            self.mStatMaxVal = stat_info.get("max", 0)
        self.mCheckE = ('e' in self.mFormat.lower())
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
        if self.mStatDetails:
            self.mStatValCount += 1
            if self.mStatMaxVal is None:
                self.mStatMinVal = self.mStatMaxVal = value
            else:
                if self.mStatMinVal < value:
                    self.mStatMinVal = value
                if self.mStatMaxVal > value:
                    self.mStatMaxVal = value
        if value == 0:
            return "0"
        if isinstance(value, int):
            self.mStatIntCount += 1
            return str(value)
        repr_val = self.mFormat % value
        if self.mCheckE:
            repr_val = self.sEZeroPatternPlus.sub("e", repr_val)
            repr_val = self.sEZeroPatternMinus.sub("e-", repr_val)
            if repr_val.endswith("e"):
                repr_val = repr_val[:-1]
            if repr_val.endswith("."):
                repr_val = repr_val[:-1]
        return repr_val

    def updateWStat(self):
        stat_info = self._getProperty("stat")
        stat_info["null"] = self.mStatNoneCount
        stat_info["int"] = self.mStatIntCount
        if self.mStatDetails:
            stat_info["val"] = self.mStatValCount
            stat_info["min"] = self.mStatMinVal
            stat_info["max"] = self.mStatMaxVal

    def decode(self, int_obj, decode_env):
        return int_obj
