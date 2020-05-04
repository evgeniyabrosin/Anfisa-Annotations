from ._codec_data import _CodecData
#===============================================
class CodecList(_CodecData):
    def __init__(self, master, parent, schema_instr, default_name):
        _CodecData.__init__(self, master, parent, schema_instr, default_name)
        self.mItemCodec = _CodecData.create(self.getMaster(), self,
            self._getProperty("item"), default_name = "")
        self._updateProperty("item", self.mItemCodec.getSchemaDescr())
        self.mStatNoneCount = 0
        self.mStatValCount = 0
        self.mStatMinL = None
        self.mStatMaxL = None
        self._onDuty()

    def getType(self):
        return "list"

    def isAtomic(self):
        return False

    def getPath(self):
        if self.getParent() is None:
            return "[]"
        return self.getParent().getPath() + "[]"

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

        items_repr = [self.mItemCodec.encode(it, encode_env)
            for it in value]
        while len(items_repr) > 0 and items_repr[-1] == "null":
            del items_repr[-1]
        return '[' + ','.join(items_repr) + ']'

    def updateWStat(self, encode_env):
        stat_info = {
            "null": self.mStatNoneCount,
            "val": self.mStatValCount,
            "min-l": self.mStatMinL,
            "max-l": self.mStatMaxL}
        self._updateProperty("stat", stat_info)

    def decode(self, int_obj, decode_env):
        if int_obj is None:
            return None
        return [self.mItemCodec.decode(it_obj, decode_env)
            for it_obj in int_obj]
