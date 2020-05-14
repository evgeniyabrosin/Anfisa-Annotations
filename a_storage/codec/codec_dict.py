from ._codec_data import _CodecData
#===============================================
class CodecDict(_CodecData):
    def __init__(self, master, parent, schema_instr, default_name):
        _CodecData.__init__(self, master, parent, schema_instr, default_name)

        self.mItemCodecs = [
            _CodecData.create(self.getMaster(), self, it_instr, "?")
            for it_instr in self._getProperty("items")]
        self.mItemNameCodecs = [(it.getName(), it)
            for it in self.mItemCodecs]
        self._updateProperty("items",
            [it.getSchemaDescr() for it in self.mItemCodecs])
        used_names = set()
        for it in self.mItemCodecs:
            it._checkNameUsage(used_names)
        stat_info = self._getProperty("stat", dict())
        self.mStatNoneCount = stat_info.get("null", 0)
        self.mStatValCount = stat_info.get("val", 0)
        self.mSerialization = None
        self._onDuty()

    def setSerialization(self, names):
        self.mSerialization = []
        used_names = set()
        for it_name, it in self.mItemNameCodecs:
            if it_name not in names:
                self.mSerialization.append(None)
                continue
            assert not it.isAggregate()
            used_names.add(it_name)
            self.mSerialization.append(it)
        lost_names = set(names) - used_names
        assert len(lost_names) == 0, (
            "Serialization of %s, lost names: %s"
            % (self.getPath(), " ".join(sorted(lost_names))))

    def getType(self):
        return "dict"

    def isAtomic(self):
        return False

    def encode(self, value, encode_env):
        if value is None:
            self.mStatNoneCount += 1
            return "null"
        if self.mSerialization is not None:
            return self.serializedEncode(value, encode_env)
        self.mStatValCount += 1
        items_repr = []
        for it_name, it in self.mItemNameCodecs:
            it_repr = "null"
            if it.isAggregate():
                it_repr = it.encode(value, encode_env)
            else:
                it_val = value.get(it_name)
                if it_val is not None:
                    it_repr = it.encode(it_val, encode_env)
            items_repr.append(it_repr)
        while len(items_repr) > 0 and items_repr[-1] == "null":
            del items_repr[-1]
        return '[' + ','.join(items_repr) + ']'

    def serializedEncode(self, value, encode_env):
        self.mStatValCount += 1
        items_repr = []
        for idx, it_val in enumerate(value):
            it = self.mSerialization[idx]
            if it is not None:
                it_repr = it.encode(it_val, encode_env)
            else:
                it_repr = "null"
            items_repr.append(it_repr)
        return '[' + ','.join(items_repr) + ']'

    def updateWStat(self):
        stat_info = self._getProperty("stat")
        stat_info["null"] = self.mStatNoneCount
        stat_info["val"] = self.mStatValCount
        for it in self.mItemCodecs:
            it.updateWStat()

    def decode(self, int_obj, decode_env):
        if int_obj is None:
            return None
        assert len(int_obj) <= len(self.mItemCodecs)
        ret = dict()
        for idx, it in enumerate(self.mItemCodecs):
            it_obj = None
            if idx < len(int_obj):
                it_obj = int_obj[idx]
            if it.isAggregate():
                if it_obj is not None:
                    ret.update(it.decode(it_obj, decode_env))
            else:
                if it_obj is not None:
                    ret[it.getName()] = it.decode(it_obj, decode_env)
                else:
                    ret[it.getName()] = None
        return ret
