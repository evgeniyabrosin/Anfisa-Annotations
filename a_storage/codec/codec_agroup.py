from ._codec_data import _CodecData
#===============================================
class CodecAGroup(_CodecData):
    def __init__(self, master, parent, schema_instr, default_name):
        self.mGroupName = "?"
        _CodecData.__init__(self, master, parent, schema_instr, default_name)
        self.mGroup = self._getProperty("group")
        self.mGroupName = self._getProperty("group-name")
        self.mItemCodecs = [
            _CodecData.create(self.getMaster(), self, it_instr, "?")
            for it_instr in self._getProperty("items")]
        if not self.mGroupName.startswith('<'):
            self.mGroupName = "<%s>" % self.mGroupName
            self._updateProperty("group-name", self.mGroupName)
        self._updateProperty("items",
            [it.getSchemaDescr() for it in self.mItemCodecs])
        used_names = set()
        for it in self.mItemCodecs:
            it._checkNameUsage(used_names)
        stat_info = self._getProperty("stat", dict())
        self.mStatValCount = stat_info.get("val", 0)
        self.mStatGrpCount = stat_info.get("groups")
        if self.mStatGrpCount is None:
            self.mStatGrpCount = {name: 0 for name in self.mGroup}
            stat_info["groups"] = self.mStatGrpCount
        self._onDuty()

    def _checkNameUsage(self, used_names):
        for name in self.mGroup:
            assert name not in used_names, (
                "Duplication name in group for codec %s" % self.getPath())
            used_names.add(name)

    def getType(self):
        return "attr-group"

    def isAtomic(self):
        return False

    def isAggregate(self):
        return True

    def getPath(self):
        if self.mParent is None:
            return "/" + self.mGroupName
        return self.mParent.getPath() + "/" + self.mGroupName

    def encode(self, value, encode_env):
        self.mStatValCount += 1
        ret_repr = []
        for name_idx, name in enumerate(self.mGroup):
            it_dict = value.get(name)
            if it_dict is None:
                continue
            self.mStatGrpCount[name] += 1
            items_repr = [str(name_idx)]
            for it in self.mItemCodecs:
                it_repr = "null"
                if it.isAggregate():
                    it_repr = it.encode(it_dict, encode_env)
                else:
                    it_val = it_dict.get(it.getName())
                    if it_val is not None:
                        it_repr = it.encode(it_val, encode_env)
                items_repr.append(it_repr)
            while len(items_repr) > 0 and items_repr[-1] == "null":
                del items_repr[-1]
            ret_repr.append('[' + ','.join(items_repr) + ']')
        return '[' + ','.join(ret_repr) + ']'

    def updateWStat(self):
        stat_info = self._getProperty("stat")
        stat_info["groups"] = self.mStatGrpCount
        stat_info["val"] = self.mStatValCount
        for it in self.mItemCodecs:
            it.updateWStat()

    def decode(self, group_obj, decode_env):
        ret = dict()
        for int_obj in group_obj:
            name = self.mGroup[int_obj[0]]
            grp_obj = dict()
            for idx, it in enumerate(self.mItemCodecs):
                it_obj = None
                if idx + 1 < len(int_obj):
                    it_obj = int_obj[idx + 1]
                if it.isAggregate():
                    if it_obj is not None:
                        grp_obj.update(it.decode(it_obj, decode_env))
                else:
                    if it_obj is not None:
                        grp_obj[it.getName()] = it.decode(it_obj, decode_env)
                    else:
                        grp_obj[it.getName()] = None
            ret[name] = grp_obj
        return ret
