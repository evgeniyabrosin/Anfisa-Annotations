import abc
#===============================================
class _Codec:
    sCreateFunc = None

    @classmethod
    def create(cls, master, parent, schema_instr, default_name = None):
        return cls.sCreateFunc(master, parent, schema_instr, default_name)

    def __init__(self, master, parent, schema_instr, default_name = None):
        self.mMaster = master
        self.mParent = parent
        self.mSchemaInstr = schema_instr
        self.mSchemaDescr = dict()
        self.mOnDuty = False
        self._getProperty("tp")
        name = self._getProperty("name", default_name)
        if default_name is not None:
            assert name == default_name

    def _getProperty(self, name, default_value = None):
        if name in self.mSchemaDescr:
            return self.mSchemaDescr[name]
        if name in self.mSchemaInstr:
            self.mSchemaDescr[name] = self.mSchemaInstr[name]
        else:
            self.mSchemaDescr[name] = default_value
        return self.mSchemaDescr[name]

    def _updateProperty(self, key, val):
        self.mSchemaDescr[key] = val

    def _checkNameUsage(self, used_names):
        name = self.getName()
        assert name, "Empty name for codec %s" % self.getPath()
        assert name not in used_names, (
            "Duplication name for codec %s" % self.getPath())
        used_names.add(name)

    def _onDuty(self):
        assert not self.mOnDuty
        unused = set(self.mSchemaInstr.keys()) - set(self.mSchemaDescr.keys())
        assert len(unused) == 0, (
            "Lost option(s) for codec %s: %s"
            % (self.getPath(), ", ".join(sorted(unused))))
        self.mOnDuty = True

    def getSchemaDescr(self):
        return self.mSchemaDescr

    def getMaster(self):
        return self.mMaster

    def getParent(self):
        return self.mParent

    def getName(self):
        return self._getProperty("name")

    def getPath(self):
        name = self.getName()
        if name:
            path_frag  = "/" + name
        else:
            path_frag = ""
        if self.mParent is None:
            return path_frag
        return self.mParent.getPath() + path_frag

    def isAggregate(self):
        return False

    @abc.abstractmethod
    def isAtomic(self):
        return None

    @abc.abstractmethod
    def getType(self):
        return None

    @abc.abstractmethod
    def updateWStat(self, encode_env):
        return None

    @abc.abstractmethod
    def encode(self, value, encode_env):
        return None

    @abc.abstractmethod
    def decode(self, repr_obj, decode_env):
        return None
